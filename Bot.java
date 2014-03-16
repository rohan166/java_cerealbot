import java.net.*;
import java.io.*;
import java.util.*;
import java.lang.*;
import jerklib.*;
import jerklib.events.*;
import jerklib.events.IRCEvent.*;
import jerklib.listeners.*;

public class Bot implements IRCEventListener
{
  private static final int MAX_USERS = 100;
  private static final String BOTNAME = "cerealbot";
  private User[] userTable = new User[MAX_USERS];
  private String lastLine;
  private int userCount;
  private Session session;
  private String channel;
  private int qcount;

  public Bot(String server, String channel)
  {
    // To connect to the irc server
    ConnectionManager conman = new ConnectionManager(new Profile("cerealbot"));
    // To hold the sessino for one server
    session = conman.requestConnection(server);
    parseQuotes();
    session.addIRCEventListener(this);
    session.setRejoinOnKick(true);
    this.channel = new String(channel);
  }

  public void parseQuotes()
  {
    int count = 0;
    File file = new File("quotes");
    BufferedReader reader;
    try
    {
      reader = new BufferedReader(new FileReader(file));
      String text = null;
      while((text = reader.readLine()) != null)
      {
        count++;
      }
      try
      {
        if(reader != null)
          reader.close();
      }
      catch(IOException e)
      {
      }
    }
    catch(FileNotFoundException e)
    {
      e.printStackTrace();
    }
    catch(IOException e)
    {
      e.printStackTrace();
    }
    this.qcount = count;
  }

  public String lmgtfy(String sender)
  {
      String backup = lastline;
      String url = "http://lmgtfy.com/?q=";
      String query = backup.replace("+", "%2B");
      query = backup.replace(" ", "+");
      query = backup.replace("*", "%2A");
      query = backup.replace("/", "%2F");
      query = backup.replace("@", "%40");
      
      url += query;
      url = sender + " : " + url;

      return url;
  }

  public int absolute(int hash)
  {
    if(hash < 0)
      return 0 - hash;
    return hash;
  }

  public void initializeTable(List<String> nicks)
  {
    int i;
    for(i = 0; i < nicks.size(); ++i)
    {
      addUser(nicks.get(i));
    }
  }

  public User getUser(String nick)
  {
    int userIndex = absolute(nick.hashCode() % MAX_USERS);
    if(userTable[userIndex] == null)
      return null;
    if(userTable[userIndex].getNick().equals(nick))
      return userTable[userIndex];
    User temp = userTable[userIndex];
    while(temp.chained)
    {
      temp = temp.getNext();
      if(temp.getNick().equals(nick))
          return temp;
    }
    return null;
  }

  public void addUser(String nick)
  {
    if(getUser(nick) == null)
    {
      int userIndex = absolute(nick.hashCode() % MAX_USERS);
      if(userTable[userIndex] == null)
      {
        userTable[userIndex] = new User(nick);
      }
      else
      {
        User temp = userTable[userIndex];
        while(temp != null && temp.chained)
        {
          temp = temp.getNext();
        }
        temp.setNext(new User(nick));
      }
    }
  }

  public void removeUser(String nick)
  {
    if(getUser(nick) != null)
    {
      int userIndex = absolute(nick.hashCode() % MAX_USERS);
      if(userTable[userIndex].getNick().equals(nick))
      {
        userTable[userIndex] = userTable[userIndex].getNext();
      }
      else
      {
        User temp = userTable[userIndex];
        User buffer;
        while(temp.chained)
        {
          buffer = temp;
          temp = temp.getNext();
          if(temp.getNick().equals(nick))
          {
            buffer.setNext(temp.getNext());
            break;
          }
        }
      }
    }
  }

  public void quote(String sender, Channel chan, String message)
  {
      String param = message.substring(6);
      while(param.length() > 0 &&
          param.substring(0,1).equals(" "))
      {
        param = param.substring(1);
      }
      int quotenum = -1;
      String num = new String("");
      if(param.length() > 0)
      {
        while(param.length() > 0 &&
            Character.isDigit(param.charAt(0)))
        {
          num = num + param.substring(0,1);
          param = param.substring(1);
        }
        try
        {
          quotenum = Integer.parseInt(num) - 1;
        }
        catch(NumberFormatException e)
        {
          e.printStackTrace();
        }
      }
      if(quotenum < 0 || quotenum > qcount)
      {
        Random rand = new Random();
        quotenum = rand.nextInt(qcount);
      }
      File file = new File("quotes");
      BufferedReader reader;
      try
      {
        reader = new BufferedReader(new FileReader(file));
        int count = 0;
        String quote;
        while((quote = reader.readLine()) != null
                && count++ < quotenum)
          ;
        chan.say(sender + " : Quote " + ++quotenum + " of " + qcount +
                  " : " + quote);
        try
        {
          if(reader != null)
            reader.close();
        }
        catch(IOException e)
        {
        }
      }
      catch(FileNotFoundException e)
      {
        e.printStackTrace();
      }
      catch(IOException e)
      {
        e.printStackTrace();
      }
  }

  public void parseURL(String message,Channel chan)
  {
      String url;
      if(message.contains("http://"))
          url = message.substring(message.indexOf("http://"));
      else
        url = message.substring(message.indexOf("https://"));
      if(url.contains(" "))
      {
        int spaceindex = url.indexOf(" ");
        url = url.substring(0,spaceindex);
      }
      URL page;
      InputStream is;
      BufferedReader br;
      String line;
      try
      {
        page = new URL(url);
        HttpURLConnection connection = (HttpURLConnection)page.openConnection();
        connection.setRequestMethod("HEAD");
        connection.connect();
        String contentType = connection.getContentType();
        if(contentType.contains("html"))
        {
          is = page.openStream();
          br = new BufferedReader(new InputStreamReader(is));
          String title = new String("");
          boolean titleCheck = false;
          int start = 0, end = 0;
          int letterCase = 0;
          while((line = br.readLine()) != null)
          {
            if(line.contains("<title>") ||
                line.contains("<TITLE"))
            {
              titleCheck = true;
              if(line.contains("<TITLE>"))
                letterCase++;
            }
            if(titleCheck)
              title = title.concat(line);
            if(line.contains("</title>") ||
                line.contains("</TITLE>"))
              break;
          }
          if(letterCase == 0)
          {
            start = title.indexOf("<title>") + 7;
            end = title.indexOf("</title>");
          }
          else
          {
            start = title.indexOf("<TITLE>") + 7;
            end = title.indexOf("</TITLE>");
          }
          title = title.substring(start,end);
          chan.say(title);
          if(is != null)
            is.close();
        }
      }
      catch(MalformedURLException mue)
      {
        mue.printStackTrace();
      }
      catch(IOException ioe)
      {
        ioe.printStackTrace();
      }
  }
  
  public void receiveEvent(IRCEvent e)
  {
    if(e.getType() == Type.CONNECT_COMPLETE)
    {
      e.getSession().join(channel);
    }
    else if(e.getType() == Type.JOIN_COMPLETE)
    {
      JoinCompleteEvent jce = (JoinCompleteEvent)e;
      jce.getChannel().say("Hello, I'm cerealbot!");
    }
    else if(e.getType() == Type.CHANNEL_MESSAGE)
    {
      MessageEvent me = (MessageEvent)e;
      Channel chan = me.getChannel();
      String sender = me.getNick();
      String message = me.getMessage();

      if(getUser(sender) == null)
        addUser(sender);
      User nick = getUser(sender);
      String messageForSender;
      while((messageForSender = nick.returnLastMessage()) != null)
        chan.say(messageForSender);
      if(message.equalsIgnoreCase(".fortune"))
      {
        try
        {
          Process pb = Runtime.getRuntime().exec("fortune");
          String line;
          BufferedReader input = new BufferedReader(new InputStreamReader(
                pb.getInputStream()));
          while((line = input.readLine()) != null)
            chan.say(line);
          input.close();
        }
        catch(IOException ioe)
        {
          ioe.printStackTrace();
        }
      }
      else if(message.equalsIgnoreCase(".help"))
      {
        session.sayPrivate(sender,"Hi, I am cerealbot!");
        session.sayPrivate(sender,"Here are the commands that I understand :");
        session.sayPrivate(sender,"1. .help - display the list of " + 
                                  "commands in a PM");
        session.sayPrivate(sender,"2. .tell <nick> <message> - post <message>" +
                                  "the next time user <nick> posts something.");
        session.sayPrivate(sender,"3. .addquote <quote> - adds <quote> to the" +
                                  " list of quotes at cerealkira.pw/quotes/");
        session.sayPrivate(sender,"4. .quote <n> - displays the <n>th quote " +
                                  "from the list of quotes at cerealkira.pw/" +
                                  "quotes/");
        session.sayPrivate(sender,"If no number is entered after <n>, a " +
                                  "random quote is displayed");
        session.sayPrivate(sender,"5. s/<text entered previously>/<text to " +
                                  "replace it with> - Replace any text you" + 
                                  " entered previously with some other text.");
        session.sayPrivate(sender,"The buffer is up to the last 10 posts");
      }
      else if(message.length() > 9 &&
          message.substring(0,10).equals(".enlighten"))
      {
        String note = message.substring(10);
        String dstNick = note.replace(" ", "");
        if(dstNick.equals(""))
            dstNick = sender;
        
        if(dstNick.equals("me"))
            chan.say(lmgtfy(sender));
        else
            chan.say("And then " + dstNick + " was enlightened.");
      }
      else if(message.length() > 2 &&
          message.substring(0,2).equals("s/"))
      {
        int index = message.substring(2).indexOf('/') + 2;
        String toReplace = message.substring(2,index);
        String replaceWith = message.substring(index+1);
        if(replaceWith.contains("/"))
        {
          replaceWith = replaceWith.substring(0,
                        replaceWith.indexOf('/'));
        }
        String suffix = sender + " meant to say : ";
        String line = nick.returnMatch(toReplace);
        if(line != null)
        {
           line = suffix + line.replaceAll("(?i)" + toReplace,replaceWith);
           chan.say(line);
        }
      }
      else if(message.length() > 6 &&
          message.substring(0,6).equals(".tell "))
      {
        String note = message.substring(6);
        while(note.substring(0,1).equals(" "))
        {
          note = note.substring(1);
        }
        String dstNick = note.substring(0,note.indexOf(" "));
        note = note.substring(note.indexOf(" "));

        if(getUser(dstNick) == null)
          addUser(dstNick);

        getUser(dstNick).reminder(sender,note);
        chan.say(sender + " : I'll pass that on when " + dstNick + 
                  " is around.");
      }
      else if(message.length() > 10 &&
          message.substring(0,9).equals(".addquote"))
      {
        BufferedWriter out = null;
        try
        {
          String quote = message.substring(9);
          FileWriter fstream = new FileWriter("quotes",true);
          out = new BufferedWriter(fstream);
          if(qcount > 0)
          {
            out.write("\n");
          }
          out.write(quote);
          qcount++;
          chan.say(sender + " : Quote added.");
        }
        catch(IOException ioe)
        {
          System.err.println("Error: " + ioe.getMessage());
        }
        finally
        {
          try
          {
            if(out != null)
              out.close();
          }
          catch(IOException ioe)
          {
          }
        }
        try
        {
           Process pb = Runtime.getRuntime().exec(
                        "cp quotes /var/www/quotes/");
        }
        catch(IOException ioe)
        {
           ioe.printStackTrace();
        }
      }
      else if(message.length() > 5 &&
          message.substring(0,6).equals(".quote"))
      {
        quote(sender,chan,message);
      }
      else
      {
        nick.addLog(message);
        lastLine = message;
      }
      if(message.contains("http://") ||
              message.contains("https://"))
      {
        parseURL(message,chan);
      }
    }
    else if(e.getType() == Type.JOIN)
    {
      JoinEvent je = (JoinEvent)e;
      addUser(je.getNick());
    }
    else if(e.getType() == Type.NICK_CHANGE)
    {
      NickChangeEvent nce = (NickChangeEvent)e;
      if(getUser(nce.getOldNick()) != null)
        getUser(nce.getOldNick()).setNick(nce.getNewNick());
    }
    else if(e.getType() == Type.NICK_LIST_EVENT)
    {
      NickListEvent nle = (NickListEvent)e;
      List<String> nicks = nle.getNicks();
      int i;
      for(i = 0; i < nicks.size(); ++i)
      {
        addUser(nicks.get(i));
      }
    }
    else if(e.getType() == Type.PART ||
            e.getType() == Type.QUIT ||
            e.getType() == Type.KICK_EVENT)
    {
      String nick;
      if(e.getType() == Type.PART)
        nick = ((PartEvent)e).getWho();
      else if(e.getType() == Type.QUIT)
        nick = ((QuitEvent)e).getNick();
      else
        nick = ((KickEvent)e).getWho();
      removeUser(nick);
    }
    else
    {
      System.out.println(e.getType() + " : " + e.getRawEventData());
    }
  }

  public static void main(String[] args)
  {
    String server = "irc.darknedgy.net";
    String channel = "#cerealtest";

    File file = new File("config");
    BufferedReader reader;
    try
    {
      reader = new BufferedReader(new FileReader(file));
      String text = null;
      while((text = reader.readLine()) != null)
      {
        if(text.substring(0,9).equals("server : "))
        {
          text = text.substring(9);
          server = new String(text);
        }
        else if(text.substring(0,10).equals("channel : "))
        {
          text = text.substring(10);
          channel = new String(text);
        }
      }
      try
      {
        if(reader != null)
          reader.close();
      }
      catch(IOException e)
      {
      }
    }
    catch(FileNotFoundException e)
    {
      e.printStackTrace();
    }
    catch(IOException e)
    {
      e.printStackTrace();
    }

    Bot cerealbot = new Bot(server,channel);
  }
}
