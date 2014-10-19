import akka.actor.Actor
import akka.actor.ActorSystem
import scala.util.Random
import akka.actor.Props
import akka.actor.ActorRef

class masterActor(numNodes: Int, numReq: Int, mySys: ActorSystem) extends Actor {

  var b = 8;
  var lenUUID: Int = 4 //length of random string can be increased to support more actors
  var i: Int = 1
  var peerList: List[ActorRef] = List()
  var count: Int = 0; //jugaad varible for initial phase of codeing....to be removed later on

  def receive = {
    //each node joins one by one
    /*case "init" =>
      if (i <= numNodes) {
        //create nodes
        var nodeid: String = getRandomID(i)
        var peer = mySys.actorOf(Props(new pastryActor(nodeid, numReq)), name = i.toString)
        peerList = peerList:+ peer
        i += 1
        //add nodes
        peer ! "join"
      }
      else {
        println("coming here")
        for(peer<-peerList){
    		peer ! "startRouting"
       }*/

    //all node join together
    case "init" =>
      for (i <- 1 to numNodes) {
        //create nodes
        var nodeid: String = getRandomID(i)
        var peer = mySys.actorOf(Props(new pastryActor(nodeid, numReq)), name = i.toString)
        peerList = peerList :+ peer
        //add nodes
      }
      for (peer <- peerList) {
        peer ! "initTable"
      }

    case "initTableDone" =>
      count += 1
      if (count == numNodes) {
        for (peer <- peerList) {
          peer ! "startRouting"
        }
      }

    case "joined" =>
      self ! "init"
      
    case _ => println("Entering default case of masterActor")

  } //receive method ends here

  def getRandomID(i: Int): String = {
    var r = new Random();
    var sb = new StringBuffer();
    while (sb.length() < lenUUID) {
      sb.append(Integer.toOctalString(r.nextInt()))
    }
    println(sb.toString().substring(0, lenUUID))
    sb.toString().substring(0, lenUUID)
  } //getRandomID method ends here
}
