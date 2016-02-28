package org.test

import java.security._
import java.security.spec.{KeySpec, RSAPublicKeySpec}
import java.util.Base64
import javax.crypto.spec.{IvParameterSpec, SecretKeySpec, PBEKeySpec}
import javax.crypto.{SecretKey, SecretKeyFactory, Cipher}
import akka.actor.{Actor, ActorSystem, Props, _}
import akka.io.IO
import akka.util.Timeout
import spray.can.Http
import spray.http.HttpResponse
import spray.httpx.RequestBuilding._
import spray.json._
import spray.routing.HttpService
import scala.concurrent.{Await, Future}
import scala.language.postfixOps
import org.test.MyJsonProtocol._

case class init(profileID: Int)

//object publicKeysClient {var data = scala.collection.mutable.Map[Int, Key]()}

object URL {
  val REGISTER = "http://127.0.0.1:8080/register"
  val INITIATE = "http://127.0.0.1:8080/initiate"
  val PROFILE = "http://127.0.0.1:8080/profile/"
  val PAGE = "http://127.0.0.1:8080/page/"
}

object FBClient extends App {

  object publicKeysClient {
    var data = scala.collection.mutable.Map[Int, Key]()
  }

  object privateKeysClient {
    var data = scala.collection.mutable.Map[Int, Key]()
  }

  val userCount = 20

  val FBClientActorSystem = ActorSystem("FBClientActorSystem")
  var user = FBClientActorSystem.actorOf(Props[User])
  var populator = FBClientActorSystem.actorOf(Props[Populator])

  //init data populator
  for (i <- 1 to userCount) {
    populator = FBClientActorSystem.actorOf(Props[Populator])
    populator ! init(i + 1000)
  }

  Thread.sleep(15000)

  //user-actor spawning for behavior simulation
  for (i <- 1 to userCount) {
    user = FBClientActorSystem.actorOf(Props[User])
    user ! init(i + 1000)
  }
}

class Populator extends Actor with HttpService {

  def receive = {
    case init(profileID: Int) =>
      init(profileID)
  }

  def init(profileID: Int) {
    //Generate keys
    val keyPair = generateKeys()

    //Save keys
    FBClient.privateKeysClient.data.update(profileID, keyPair.getPrivate)
    FBClient.publicKeysClient.data.update(profileID, keyPair.getPublic)

    //Send PUBLIC key to server
    val fact: KeyFactory = KeyFactory.getInstance("RSA")
    val pub: RSAPublicKeySpec = fact.getKeySpec(keyPair.getPublic, classOf[RSAPublicKeySpec])
    val publicKeyInfo = PublicKeyInfo(profileID.toString, new String(Base64.getEncoder.encode(pub.getModulus.toByteArray)),
      new String(Base64.getEncoder.encode(pub.getPublicExponent.toByteArray))).toJson.prettyPrint
    Library.httpRequestResponse(URL.REGISTER, HTTPMETHOD.PUT, publicKeyInfo)

    //Create Profile
    val profileData = "{\n    \"id\": \""+profileID.toString+"\",\n    \"name\": \"Bhaveek-"+profileID.toString+"\",\n  " +
      "  \"gender\": \"Male\",\n    \"age\": \"22\",\n    \"location\": \"Gainesville, FL\",\n    \"interests\": \"dance, gaming\"," +
      "\n    \"birthday\": \"04/17/1990\",\n    \"email\": \"nukulshah@ufl.edu\",\n    \"friendList\": \"1002,1003,1004,1005\"," +
      "\n    \"postList\": \"\",\n    \"additional_field1\": \"\",\n    \"additional_field2\": \"\",\n    \"additional_field3\": \"\"," +
      "\n    \"additional_field4\": \"\",\n    \"additional_field5\": \"\",\n    \"additional_field6\": \"\",\n    \"additional_field7\": \"\"," +
      "\n    \"additional_field8\": \"\",\n    \"additional_field9\": \"\",\n    \"additional_field10\": \"\"\n}"

    Library.httpRequestResponse(URL.PROFILE+profileID.toString, HTTPMETHOD.PUT, profileData)
 }

  def generateKeys(): KeyPair = {
    val RSAKeyGenerator = KeyPairGenerator.getInstance("RSA")
    RSAKeyGenerator.initialize(2048)
    RSAKeyGenerator.genKeyPair
  }

  def actorRefFactory = context
}

class User extends Actor with HttpService {

  var profileID: Int = 0
  var publicKey: Key = null
  var privateKey: Key = null
  var hashedPublicKey = ""
  var hashedPrivateKey = ""
  var sessionRandomNumber = ""

  def receive = {
    case init(profileID: Int) =>
      this.profileID = profileID
      this.publicKey = FBClient.publicKeysClient.data.apply(profileID)
      this.privateKey = FBClient.privateKeysClient.data.apply(profileID)
      this.hashedPublicKey = Library.getHash(publicKey,"")
      this.hashedPrivateKey = Library.getHash(privateKey,"")

      // Let's go!
      startActivity()
  }

  def startActivity() {

    //View Profile
    Library.httpRequestResponse(URL.PROFILE+profileID.toString, HTTPMETHOD.GET, "")

    //New Post
    sendPost(profileID+","+"1002,1003")

    //View Post
    viewPost()

  }

  def requestSession(activity: String) = {
    val response = Library.httpRequestResponse(URL.INITIATE, HTTPMETHOD.PUT, profileID.toString)
    this.sessionRandomNumber = Library.decryptData(response, FBClient.privateKeysClient.data.apply(profileID), "", "RSA")
    println(profileID + " UserSession Random Number| " + sessionRandomNumber + activity)
  }

  def sendPost(to: String) = {
    var recipients: Array[String] = null

    if (to != "") {
      recipients = to.split(",")
    }

    if (recipients != null) {
      for((recipient, index) <- recipients.view.zipWithIndex) {

        //Request Session Random Number
        requestSession(" | New Post")

        //Draft Post
        val post = "{\n  \"timestamp\": \"12/17/2015\",\n  \"additional_field3\": \"\",\n  \"from_id\": \""+profileID.toString+"\"," +
          "\n  \"additional_field1\": \"\",\n  \"is_deleted\": \"\",\n  \"is_private\": \"\",\n  \"page_id\": \"\",\n  " +
          "\"additional_field2\": \"\",\n  \"id\": \"1001\",\n  \"to_id\": \""+recipient+"\",\n  \"additional_field5\": \"\",\n  " +
          "\"message\": \"Hello!\",\n  \"additional_field4\": \"\"\n}"

        //Check if public key is compromised; if so, retry; if not, only then continue
        if (Library.checkKeyAuthenticity(publicKey, hashedPublicKey)) {

          //AES encryption with shared key
          var encryptedData = Library.encryptData(post, null, Library.getHash(null,"0000"), "AES")

          //RSA encryption with recipient's PUBLIC KEY
          encryptedData = Library.encryptData(post, FBClient.publicKeysClient.data.apply(Integer.parseInt(recipient)), "", "RSA")

          //Metadata includes recipient's ID
          val metadata = recipient

          //Package to send
          val dataCapsule = (new DataCapsule(metadata, encryptedData)).toJson.prettyPrint

          //AES encryption of package with session random number
          val encryptedDataCapsule = Library.encryptData(dataCapsule, null, Library.getHash(null, sessionRandomNumber), "AES")

          Library.httpRequestResponse(URL.PROFILE+profileID.toString+"?posts", HTTPMETHOD.PUT, encryptedDataCapsule)
        } else {
          println(profileID + " | " + "Possible man-in-the-middle attack!! Retrying...")
          startActivity()
        }
      }
    }

  }

  def viewPost() = {
    //Request session random number
    requestSession(" | View Post")

    //AES encryption of package with session random number
    val encryptedDataCapsule = Library.encryptData(profileID.toString, null, Library.getHash(null, sessionRandomNumber), "AES")

    //Check if public key is compromised; if so, retry; if not, only then continue
    if (Library.checkKeyAuthenticity(publicKey, hashedPublicKey)) {

      val response = Library.httpRequestResponse(URL.PROFILE + profileID.toString + "?posts", HTTPMETHOD.GET, encryptedDataCapsule)

      if (response.contains("|||")) {
        val posts_list = response.split("|||")

        for ((post, index) <- posts_list.view.zipWithIndex) {
          // Decrypting first with PRIVATE KEY
          var postData = Library.decryptData(post, privateKey, "", "RSA")

          // Decrypting then with global AES key
          postData = Library.decryptData(postData, null, Library.getHash(null, "0000"), "AES")

        }
      } else {
        println("No posts yet")
      }
    } else {
      println(profileID + " | " + "Possible man-in-the-middle attack!! Retrying...")
      startActivity()
    }
  }

  def actorRefFactory = context
}
