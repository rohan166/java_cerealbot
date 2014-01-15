import java.util.*;

public class User
{
   private static final int MAX_MESSAGES = 10;
   public boolean chained = false;
   private String nick;
   private ArrayList<String> log = new ArrayList<String>();
   private ArrayList<String> messages = new ArrayList<String>();
   private User next = null;
   
   public User(String nick)
   {
      this.nick = new String(nick);
   }
   
   public void addLog(String userMessage)
   {  
      log.add(new String(userMessage));
      if(log.size() > MAX_MESSAGES)
      {
         log.remove(0);
      }
   }

   public void reminder(String sender, String message)
   {
     String note = sender + " said :" + message;
     messages.add(note);
   }

   public String returnLastMessage()
   {
     if(messages.size() < 1)
       return null;
     int lastIndex = messages.size() - 1;
     String message = new String(messages.get(lastIndex));
     message = getNick() + " : " + message;
     messages.remove(lastIndex);
     return message;
   }

   public String returnMatch(String pattern)
   {
      for(int i = log.size() - 1; i >= 0; --i)
      {
         String message = log.get(i);
         if(message.toLowerCase().contains(pattern.toLowerCase()))
         {
            return message;
         }
      }
      return null;
   }

   public void setNick(String nick)
   {
      this.nick = new String(nick);
   }

   public String getNick()
   {
      return nick;
   }

   public void setNext(User next)
   {
      this.next = next;
      if(next == null)
         chained = false;
      else
         chained = true;
   }

   public User getNext()
   {
      return next;
   }
}
