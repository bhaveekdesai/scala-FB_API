package org.test

import spray.json.DefaultJsonProtocol

case class Profile (id: String, name: String, gender: String, age: String, location: String, interests: String,
                    birthday: String, email: String, friendList: String, postList: String, additional_field1: String,
                    additional_field2: String, additional_field3: String, additional_field4: String,
                    additional_field5: String, additional_field6: String, additional_field7: String,
                    additional_field8: String, additional_field9: String, additional_field10: String)
case class Page (id: String, owner_id: String, name: String, description: String, postList: String,
                 additional_field1: String, additional_field2: String, additional_field3: String,
                 additional_field4: String, additional_field5: String)
case class FriendList (friendList: Array[Profile])
case class Post (id: String, from_id: String, to_id: String, page_id: String, message: String, timestamp: String,
                 is_deleted: String, is_private: String, additional_field1: String, additional_field2: String,
                 additional_field3: String, additional_field4: String, additional_field5: String)
case class PostList (postList: Array[Post])
case class PublicKeyInfo (profileID: String, modulus: String, exponent: String)
case class DataCapsule (metadata: String, encryptedData: String)

object MyJsonProtocol extends DefaultJsonProtocol {
  implicit val profileFormat = jsonFormat20(Profile)
  implicit val pageFormat = jsonFormat10(Page)
  implicit val friendListFormat = jsonFormat1(FriendList)
  implicit val postFormat = jsonFormat13(Post)
  implicit val postsListFormat = jsonFormat1(PostList)
  implicit val publicKeyInfoFormat = jsonFormat3(PublicKeyInfo)
  implicit val dataCapsuleFormat = jsonFormat2(DataCapsule)
}