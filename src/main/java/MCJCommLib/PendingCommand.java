package MCJCommLib;

public class PendingCommand {
  
  private StringBuilder str;
  private boolean done;
  
  public PendingCommand() {
    this.str = new StringBuilder();
  }
  
  public void append(String str) {
    this.str.append(str);
  }

  public void appendError(String str) {
    str = "$ERRORFRAME$" + str;
    append(str);
  }
  
  public void setDone(boolean done) {
    this.done = done;
  }
  
  public boolean isDone() {
    return this.done;
  }
  
  @Override
  public String toString() {
    return this.str.toString().trim(); // trim to remove /r/n from the end
  }
  
}
