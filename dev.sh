
# mvn clean compile exec:java
# mvn exec:java -Dexec.mainClass="com.hashedalgorithm.playerchat.services.Server"

PROJECT_DIR="/Users/hashedalgorithm/Projects/360t-assignment/playerchat/"
cd "$PROJECT_DIR" || exit

open_terminal() {
  if [[ "$OSTYPE" == "linux-gnu"* ]]; then
    # Linux (using gnome-terminal)
    gnome-terminal -- bash -c "$1; exec bash"
  elif [[ "$OSTYPE" == "darwin"* ]]; then
    # macOS (using terminal)
    osascript -e "tell application \"Terminal\" to do script \"cd '$PROJECT_DIR'; $1\""
  else
    echo "Unsupported OS."
  fi
}

#echo "Starting the server..."
#open_terminal "mvn exec:java -Dexec.mainClass='com.hashedalgorithm.playerchat.server.App'; exec bash"


# sleep 5 #Some time for the server start up

 # Client 1 - in new terminal
# echo "Starting Client 1..."
# sleep 5
# open_terminal "mvn exec:java -Dexec.mainClass='com.hashedalgorithm.playerchat.client.App'; exec bash"

 # Client 2 - in new terminal
 echo "Starting Client 2..."
 open_terminal "mvn exec:java -Dexec.mainClass='com.hashedalgorithm.playerchat.client.App'; exec bash"