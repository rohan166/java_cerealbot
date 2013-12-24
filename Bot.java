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
  private int userCount;
  private Session session;
  private String channel;
  public Bot(String server, String channel)
  {
    // To connect to the irc server
    ConnectionManager conman = new ConnectionManager(new Profile("cerealbot"));
    // To hold the sessino for one server
    session = conman.requestConnection(server);
    session.addIRCEventListener(this);
    session.setRejoinOnKick(true);
    this.channel = new String(channel);
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
      boolean addLog = true;
      User nick = getUser(sender);
      String messageForSender;
      while((messageForSender = nick.returnLastMessage()) != null)
        chan.say(messageForSender);
      if(message.equalsIgnoreCase(".fortune"))
      {
        addLog = false;
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
        session.sayPrivate(sender, "2. .tell <nick> <message> - post <message>" +
                                   "the next time user <nick> posts something.");
        session.sayPrivate(sender,"3. s/<text entered previously>/<text to " +
                                  "replace it with> - Replace any text you" + 
                                  " entered previously with some other text.");
        session.sayPrivate(sender,"The buffer is up to the last 10 posts");
      }
      else if(message.length() > 2 &&
          message.substring(0,2).equals("s/"))
      {
        addLog = false;
        int index = message.substring(2).indexOf('/') + 2;
        String toReplace = message.substring(2,index);
        String replaceWith = message.substring(index+1);
        String suffix = sender + " meant to say : ";
        String line = nick.returnMatch(toReplace);
        if(line != null)
          line = suffix + line.replaceAll(toReplace,replaceWith);
        chan.say(line);
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
        if(getUser(dstNick) != null)
          getUser(dstNick).reminder(sender,note);
      }
      else if(message.contains("http://") ||
              message.contains("https://"))
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
      if(addLog)
      {
        nick.addLog(message);
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

    Bot cerealbot = new Bot(server,channel);
  }
}
