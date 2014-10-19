import akka.actor.Actor

class pastryActor(nodeid: String, numReq: Int) extends Actor{

  def receive = {
	//each node joins one by one  
  	case "join" =>
      println("Node "+ self.path + " joined")
      sender ! "joined"
    
    case "startRouting" =>
      println("Node "+ self.path + " started routing")
     
    //all node join together... to be removed later
    case "initTable" =>
      println("Node "+ self.path + " initializing table")
      
      sender ! "initTableDone"
      
    case _ => println("Entering default case of pastryActor")
      
  }
}
