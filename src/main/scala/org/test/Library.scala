package org.test

import java.math.BigInteger
import java.security.{Key, _}
import java.security.spec.KeySpec
import java.util.Base64
import javax.crypto.spec.{IvParameterSpec, PBEKeySpec, SecretKeySpec}
import javax.crypto.{Cipher, SecretKey, SecretKeyFactory}
import akka.actor.ActorSystem
import akka.io.IO
import akka.pattern.ask
import akka.util.Timeout
import spray.can.Http
import spray.http.HttpResponse
import spray.httpx.RequestBuilding._
import scala.concurrent.duration._
import scala.concurrent.{Future, _}
import scala.language.postfixOps

object HTTPMETHOD {
  val GET = "get"
  val PUT = "put"
  val POST = "post"
  val DELETE = "delete"
}

object Library {
  def getHash(key: Key, keyString: String): String = {
    var hashedKey = ""
    if (null == key) {
      hashedKey = MessageDigest.getInstance("SHA-256").digest(keyString.getBytes("UTF-8")).map("%02x".format(_)).mkString
    } else {

      val byteKey = key.getEncoded
      val stringKey: StringBuffer = new StringBuffer
      for (i <- 0 to byteKey.length - 1) {
        stringKey.append(Integer.toHexString(0x0100 + (byteKey(i) & 0x00FF)).substring(1))
      }
      hashedKey = MessageDigest.getInstance("SHA-256").digest(stringKey.toString.getBytes("UTF-8")).map("%02x".format(_)).mkString
    }
    hashedKey
  }

  def checkKeyAuthenticity(key: Key, hashKey: String): Boolean = {
    if (getHash(key,"").equalsIgnoreCase(hashKey)) {
      true
    } else {
      false
    }
  }

  def encryptData(data: String, key: Key, keyString: String, algorithm: String): String = {
    if (algorithm.equalsIgnoreCase("RSA")) {
      try {
        val cipher: Cipher = Cipher.getInstance("RSA")
        val byteData = data.getBytes
        cipher.init(Cipher.ENCRYPT_MODE, key)
        new String(Base64.getEncoder.encode(cipher.doFinal(byteData)))
      } catch {
        case ex: Exception =>
          println(ex)
          "An error occurred"
      }
    }
    else if (algorithm.equalsIgnoreCase("AES")) {
      val factory: SecretKeyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
      val salt = Array[Byte](0.toByte)
      val spec: KeySpec = new PBEKeySpec(keyString.toCharArray, salt, 65536, 256)
      val secret: SecretKey = new SecretKeySpec(factory.generateSecret(spec).getEncoded, "AES")

      val cipher: Cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
      cipher.init(Cipher.ENCRYPT_MODE, secret)

      val iv: Array[Byte] = cipher.getParameters.getParameterSpec(classOf[IvParameterSpec]).getIV()
      new String(Base64.getEncoder.encode(cipher.doFinal(data.getBytes("UTF-8"))))
    } else {
      ""
    }
  }

  def decryptData(data: String, key: Key, keyString: String, algorithm: String): String = {
    if (algorithm.equalsIgnoreCase("RSA")) {
      try {
        val cipher: Cipher = Cipher.getInstance("RSA")
        val byteData = Base64.getDecoder.decode(data.getBytes)
        cipher.init(Cipher.DECRYPT_MODE, key)
        return new String(cipher.doFinal(byteData))
      } catch {
        case ex: Exception =>
          println(ex)
          "An error occurred"
      }
    } else if (algorithm.equalsIgnoreCase("AES")) {
      try {
        val factory: SecretKeyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
        val cipher: Cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        val salt = Array[Byte](0.toByte)
        val spec: KeySpec = new PBEKeySpec(keyString.toCharArray, salt, 65536, 256)
        val secret: SecretKey = new SecretKeySpec(factory.generateSecret(spec).getEncoded, "AES")
        val iv: Array[Byte] = cipher.getParameters.getParameterSpec(classOf[IvParameterSpec]).getIV
        cipher.init(Cipher.DECRYPT_MODE, secret, new IvParameterSpec(iv))
        return new String(cipher.doFinal(Base64.getDecoder.decode(data)))
      } catch {
        case ex: Exception =>
          println(ex)
          "An error occurred"
      }
    } else {
      ""
    }
  }

  def secureRandomNumberGenerator (size: Int): BigInteger = {
    val secureRandomGenerator: SecureRandom = SecureRandom.getInstance("SHA1PRNG")
    val randomBytes = Array.ofDim[Byte](size)
    secureRandomGenerator.nextBytes(randomBytes)
    new BigInteger(randomBytes)
  }

  @throws(classOf[java.io.IOException])
  def httpRequestResponse(url: String, method: String, entityBody: String): String = {
    implicit val actorSystem = ActorSystem("FBClientActorSystem")
    implicit val timeout = Timeout(5.seconds)
    var response: Future[HttpResponse] = null

    if (method == HTTPMETHOD.GET) {
      response = ask(IO(Http), Get(url)).mapTo[HttpResponse]
    } else if (method == HTTPMETHOD.PUT) {
      response = ask(IO(Http), Put(url, entityBody)).mapTo[HttpResponse]
    } else if (method == HTTPMETHOD.DELETE) {
      response = ask(IO(Http), Delete(url, entityBody)).mapTo[HttpResponse]
    } else if (method == HTTPMETHOD.POST) {

    }
    val result = Await.result(response, timeout.duration)
    result.entity.asString
  }
}