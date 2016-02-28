package org.test

import java.io.IOException
import java.math.BigInteger
import java.security.{MessageDigest, SecureRandom, KeyFactory, Key}
import java.security.spec.{KeySpec, RSAPublicKeySpec}
import java.util.Base64
import javax.crypto.{SecretKey, SecretKeyFactory, Cipher}
import javax.crypto.spec.{IvParameterSpec, PBEKeySpec, SecretKeySpec}
import akka.actor.{Actor, ActorSystem, Props}
import akka.io.IO
import akka.pattern.ask
import akka.util.Timeout
import org.test.MyJsonProtocol._
import spray.can.Http
import spray.http.MediaTypes._
import spray.json._
import spray.routing.HttpService
import sun.misc.BASE64Decoder
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.Random

object profiles {var data = scala.collection.mutable.Map[String, String]()}
object posts {var data = scala.collection.mutable.Map[String, String]()}
object postsMetadata {var data = scala.collection.mutable.Map[String, String]()}
object pages {var data = scala.collection.mutable.Map[String, String]()}
object randomNumbers {var data = scala.collection.mutable.Map[String, BigInteger]()}
object publicKeys {var data = scala.collection.mutable.Map[Int, Key]()}
object currentProfileId {var data = 1001}
object currentPostId {var data = 1001}

object launcher extends App {
  implicit val actorSystem = ActorSystem("launcher")
  val service = actorSystem.actorOf(Props[FBServiceActor],"fb-rest-service")

  implicit val timeout = Timeout(10.seconds)
  IO(Http) ? Http.Bind(service, interface = "localhost", port = 8080)
}

class FBServiceActor extends Actor with RouteService {
  def actorRefFactory = context
  def receive = runRoute(profileRoute ~ pageRoute ~ sessionRequestRoute ~ registerRoute)
}

trait RouteService extends HttpService {

  val TRUE = "{\"success\": \"true\"}".parseJson.prettyPrint
  val FALSE = "{\"success\": \"false\"}".parseJson.prettyPrint

  val PROFILE = "profile"
  val POST = "post"
  val PAGE = "page"
  val SESSION = "initiate"
  val REGISTER = "register"

  val profileRoute = {
    //Scope of profile
    //Read profile posts
    (path(PROFILE / Segment) & get) { profileID =>
      parameter('posts) { x =>
        respondWithMediaType(`application/json`) {
          val profile = readFromFile(PROFILE, profileID).convertTo[Profile]

          // Fetch session random number
          val AESKey = manageRandomNumbers(profileID)

          // AES Decryption to get plaintext metadata and encrypted data
          val requesterID = Library.decryptData(x, null, AESKey.toString, "AES")

          if (requesterID == profileID) {
            //Fetch post ids of the profile
            val post_ids = postsMetadata.data.apply(profileID).split(",")

            //If there are no posts
            if (post_ids(0) == "") {
              println("[LOG] [Posts View] [empty] " + profile.name)
              complete("No posts yet!")
            }
            else {
              var postList = ""
              for ((post, index) <- post_ids.view.zipWithIndex) {
                postList = postList + post + "|||"
              }

              //Log & send response
              println("[LOG] [Posts View] " + profile.name)
              complete(postList)

            }
          } else {
            //Log & send response
            println("[LOG] [RESTRICTED] [Posts View] " + profile.name)
            complete(FALSE)
          }
        }
      }~
        //Read FriendList info
        parameter('friends) { x =>
          respondWithMediaType(`application/json`) {

            //Fetch profile ids of friends
            val profile = readFromFile(PROFILE, profileID).convertTo[Profile]
            val friend_ids = profile.friendList.split(",")

            //Fetch profiles
            val friends_info = Array.ofDim[Profile](friend_ids.length)
            for ((friend, index) <- friend_ids.view.zipWithIndex) {
              friends_info(index) = readFromFile(PROFILE, friend).convertTo[Profile]
            }
            val friends_info_collated = FriendList(friends_info)
            val friendList = friends_info_collated.toJson.prettyPrint

            //Log & send response
            println("[LOG] [Friend-list View] "+profile.name)
            complete(friendList)
          }
        }~
        //Read Profile info
        get {
          respondWithMediaType(`application/json`) {
            try {
              val profile = readFromFile(PROFILE, profileID).convertTo[Profile]

              //Log & send response
              println("[LOG] [Profile view] "+profile.name)
              complete(profile.toJson.prettyPrint)
            } catch {
              //Log & send response in case of any error
              case ex: Exception => println("[LOG] [ERROR] [Profile View] Couldn't find profile with ID: "+profileID)
                complete("Sorry! Couldn't find profile with ID: "+profileID)
            }
          }
        }
    }~
      //Update post/profile
      (path(PROFILE / Segment) & put) { profileID =>
        //Update or new post
        parameter('posts) { x =>
          entity(as[String]) {
            post_info => {
              respondWithMediaType(`application/json`) {
                // Fetch session random number
                val AESKey = manageRandomNumbers(profileID)

                // AES Decryption to get plaintext metadata and encrypted data
                val post_infoData = Library.decryptData(post_info, null, AESKey.toString ,"AES").parseJson.convertTo[DataCapsule]

                val metadata = post_infoData.metadata
                val encryptedData = post_infoData.encryptedData

                // Add post
                addPostID(profileID, metadata, encryptedData)

                //Log & send response
                val profile = readFromFile(PROFILE, profileID).convertTo[Profile]
                println("[LOG] [New Post] by "+profile.name)
                complete(TRUE)
              }
            }
          }
        }~
        //Update profile
          put {
            entity(as[String]) {
              profile_updated_info => {
                respondWithMediaType(`application/json`) {

                  try {
                    writeToFile(PROFILE, profileID, profile_updated_info)

                    //Log & send response
                    val profile = readFromFile(PROFILE, profileID).convertTo[Profile]
                    println("[LOG] [Profile Update] "+profile.name)
                    complete(TRUE)
                  } catch {
                    //Log error & send response
                    case ex: Exception => println("[LOG] [ERROR] [Profile Update] : There was some issue updating profile with ID: " + profileID)
                      complete("Sorry! There was some issue updating profile with ID: " + profileID)
                  }
                }
              }
            }
          }
      }~
      //Delete post
      (path("profile" / Segment) & delete) { profileID =>
        val profile = readFromFile(PROFILE, profileID).convertTo[Profile]
        parameter('posts) { x =>
          entity(as[String]) {
            postInfo => {
              respondWithMediaType(`application/json`) {
                // Fetch session random number
                val AESKey = manageRandomNumbers(profileID)

                // AES Decryption to get plaintext metadata and encrypted data
                val post_infoData = Library.decryptData(postInfo, null, AESKey.toString ,"AES").parseJson.convertTo[DataCapsule]

                val postID = post_infoData.metadata

                if (!postsMetadata.data.get(profileID).contains(postID)) {
                  //Log & send response
                  println("[LOG] [ERROR] [Post Delete] by "+profile.name)
                  complete(FALSE)
                }

                posts.data.remove(postID)

                //Log & send response
                println("[LOG] [Post Delete] by "+profile.name)
                complete(TRUE)
              }
            }
          }
        }
      }
  }

  val pageRoute = {
    //Scope of page
    //Read page posts
    (path(PAGE / Segment) & get) { pageID =>
      (parameter('posts) & get) { x =>
        respondWithMediaType(`application/json`) {
          val page = readFromFile(PROFILE, pageID).convertTo[Page]

          // Fetch session random number
          val AESKey = manageRandomNumbers(pageID)

          // AES Decryption to get plaintext metadata and encrypted data
          val post_infoData = Library.decryptData(x, null, AESKey.toString ,"AES").parseJson.convertTo[DataCapsule]

          val metadata = post_infoData.metadata

          //Fetch post ids of the profile
          val post_ids = postsMetadata.data.apply(pageID).split(",")

          //If there are no posts
          if (post_ids(0) == "") {
            println("[LOG] [Posts View] [empty] "+page.name)
            complete("No posts yet!")
          }

          else {
            var postList = ""
            for ((post, index) <- post_ids.view.zipWithIndex) {
              postList = postList + post + "|||"
            }
            //Log & send response
            println("[LOG] [Page Posts View] "+page.name)
            complete(postList)
          }
        }
      }~
      //Read page info
      get {
          respondWithMediaType(`application/json`) {
            try {
              val page = readFromFile(PAGE, pageID).convertTo[Page]
              val page_info = page.toJson.prettyPrint

              //Log & send response
              println("[LOG] [Page View] "+page.name)
              complete(page_info)
            } catch {
              //Log error & send response
              case ex: Exception => println("[LOG] [Page View] [ERROR] Couldn't find page with ID: " + pageID)
                complete("Sorry! Couldn't find page with ID: " + pageID)
            }
          }
        }
    }~
      //Update page
      (path(PAGE / Segment) & put) { pageID =>
        put {
          entity(as[String]) {
            page_updated_info => {
              respondWithMediaType(`application/json`) {

                try {
                  writeToFile(PAGE, pageID, page_updated_info)

                  //Log & send response
                  val page = readFromFile(PAGE, pageID).convertTo[Page]
                  println("[LOG] [Page Update] "+page.name)
                  complete(TRUE)
                } catch {
                  //Log error & send response
                  case ex: Exception => println("[LOG] [Page Update] [ERROR] There was some issue updating page with ID: " + pageID)
                    complete("Sorry! There was some issue updating page with ID: " + pageID)
                }
              }
            }
          }
        }
      }
  }

  val sessionRequestRoute = {
    (path(SESSION) & put) {
      entity(as[String]) {
        profileID => {
          respondWithMediaType(`application/json`) {
            // Secure Random Number Generator
            val SRN: BigInteger = Library.secureRandomNumberGenerator(8)

            // Save SRN
            randomNumbers.data.update(profileID.toString, SRN)

            // ENCRYPT SRN with requester's PUBLIC KEY
            val requesterPublicKey: Key = publicKeys.data.apply(Integer.parseInt(profileID))
            val response = Library.encryptData(SRN.toString, requesterPublicKey, "", "RSA")

            println(profileID + " | " + SRN.toString + " | "+ response)
            complete(response)
          }
        }
      }
    }
  }

  val registerRoute = {
    (path(REGISTER) & put) {
      entity(as[String]) {
        publicKeyData => {
          respondWithMediaType(`application/json`) {
            val publicKeyInfo = publicKeyData.parseJson.convertTo[PublicKeyInfo]
            val spec: RSAPublicKeySpec = new RSAPublicKeySpec(new BigInteger(Base64.getDecoder.decode(publicKeyInfo.modulus.getBytes)),
              new BigInteger(Base64.getDecoder.decode(publicKeyInfo.exponent.getBytes)))
            val factory: KeyFactory = KeyFactory.getInstance("RSA")
            val publicKey: Key = factory.generatePublic(spec)

            publicKeys.data.update(Integer.parseInt(publicKeyInfo.profileID),publicKey)
            complete(TRUE)
          }
        }
      }
    }
  }

  def readFromFile(entity: String, id: String): JsValue = {
    var file_contents: JsValue = null

    if(entity == PROFILE) {
      file_contents = profiles.data.get(id).mkString.parseJson
    } else if(entity == POST) {
      file_contents = posts.data.get(id).mkString.parseJson
    } else if(entity == PAGE) {
      file_contents = pages.data.get(id).mkString.parseJson
    }

    file_contents
  }

  def writeToFile(entity: String, id: String, content: String) = {
    //Write/Update JSON files
    var file_contents = content
    if (entity == POST) {
      val previous_post_id = content.parseJson.convertTo[Post].id
      file_contents = file_contents.replaceAll("\"id\": " + "\""+previous_post_id+"\"", "\"id\": " + "\""+id+"\"")
    }

    if(entity == PROFILE) {
     profiles.data.update(id, file_contents)
    } else if(entity == POST) {
     posts.data.update(id, file_contents)
    } else if(entity == PAGE) {
     pages.data.update(id, file_contents)
    }

  }

  def addPostID(profileID: String, metadata: String, encryptedData: String) = {
    val post_id = profileID + "_" + Library.secureRandomNumberGenerator(4).toString
    val current_list = postsMetadata.data.applyOrElse(metadata, null)
    if (null == current_list) {
      postsMetadata.data.update(metadata,post_id+",")
    } else {
      postsMetadata.data.update(metadata,current_list+post_id+",")
    }
    posts.data.update(post_id, encryptedData)
  }

  def manageRandomNumbers(profileID: String): String = {
    val AESKey = randomNumbers.data.applyOrElse(profileID, null)
    if (null == AESKey) {
      complete("Your session is invalid. Please try again")
    }

    // Flush out Random Number
    randomNumbers.data.remove(profileID)
    AESKey.toString
  }
}