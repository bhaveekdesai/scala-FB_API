#!/bin/bash

requestCount=1
lastProfile=1012
lastPage=1002
sleepTime=1
#(echo "beginning >>" >> results.log)
## Post
# New post- own wall
if [ $2 == "new_post_own" ]; then
	while (true); do
		 randomProfile=$(shuf -i 1001-$lastProfile -n 1)
		
		 (sed -i -e 's/\"from_id\": \"\"/\"from_id\": \"'$randomProfile'\"/g' DATA1.json)
		
		 result=$(curl -sL -w "%{http_code}" -H "Content-Type: application/json" -d @DATA1.json -X PUT http://127.0.0.1:8080/profile/$randomProfile?posts -o /dev/null)
		 
		 (echo "$1 : $requestCount : $result" >> results.log)
		 requestCount=$((requestCount+1))
		
		 (sed -i -e 's/\"from_id\": \"'$randomProfile'\"/\"from_id\": \"\"/g' DATA1.json)
		 (sleep $sleepTime)
(sleep $sleepTime)
	done
fi

# New post - friend's wall
if [ $2 == "new_post_friend" ]; then
	while (true); do
		 randomProfileFrom=$(shuf -i 1001-$lastProfile -n 1)
		 randomProfileTo=$(shuf -i 1001-$lastProfile -n 1)
		 
		 (sed -i -e 's/\"from_id\": \"\"/\"from_id\": \"'$randomProfileFrom'\"/g' DATA2.json)
		 (sed -i -e 's/\"to_id\": \"\"/\"to_id\": \"'$randomProfileTo'\"/g' DATA2.json)
		 
		 result=$(curl -sL -w "%{http_code}" -H "Content-Type: application/json" -d @DATA2.json -X PUT http://127.0.0.1:8080/profile/$randomProfileFrom?posts -o /dev/null)
		 
		 (echo "$1 : $requestCount : $result" >> results.log)
		 requestCount=$((requestCount+1))
		 
		 (sed -i -e 's/\"from_id\": \"'$randomProfileFrom'\"/\"from_id\": \"\"/g' DATA2.json)
		 (sed -i -e 's/\"to_id\": \"'$randomProfileTo'\"/\"to_id\": \"\"/g' DATA2.json)
(sleep $sleepTime)
	done
fi

# New post - page's wall
if [ $2 == "new_post_page" ]; then
	while (true); do
		 randomProfileFrom=$(shuf -i 1001-$lastProfile -n 1)
		 randomPageTo=$(shuf -i 1001-$lastPage -n 1)
		 
		 (sed -i -e 's/\"from_id\": \"\"/\"from_id\": \"'$randomProfileFrom'\"/g' DATA3.json)
		 (sed -i -e 's/\"page_id\": \"\"/\"page_id\": \"'$randomProfileTo'\"/g' DATA3.json)
		 
		 result=$(curl -sL -w "%{http_code}" -H "Content-Type: application/json" -d @DATA3.json -X PUT http://127.0.0.1:8080/profile/$randomProfileFrom?posts -o /dev/null)
		 
		 (echo "$1 : $requestCount : $result" >> results.log)
		 requestCount=$((requestCount+1))
		 
		 (sed -i -e 's/\"from_id\": \"'$randomProfileFrom'\"/\"from_id\": \"\"/g' DATA3.json)
		 (sed -i -e 's/\"page_id\": \"'$randomProfileTo'\"/\"page_id\": \"\"/g' DATA3.json)
(sleep $sleepTime)
	done
fi

## Profile
# View profile
if [ $2 == "view_profile" ]; then
	while (true); do
		 randomProfile=$(shuf -i 1001-$lastProfile -n 1)
		 
		 result=$(curl -sL -w "%{http_code}" -X GET http://127.0.0.1:8080/profile/$randomProfile -o /dev/null)
		 (echo "$1 : $requestCount : $result" >> results.log)
		 requestCount=$((requestCount+1)) 
(sleep $sleepTime)
	done
fi

# Update profile
if [ $2 == "update_profile" ]; then
	while (true); do
		 randomProfile=$(shuf -i 1001-$lastProfile -n 1)
		 
		 (cp -f ../../main/resources/files/profiles/$randomProfile.json DATA4.json)
		 (sed -i -e 's/\"additional_field10\": \"\"/\"additional_field10\": \"updated\"/g' DATA4.json)
		 
		 result=$(curl -sL -w "%{http_code}" -H "Content-Type: application/json" -d @DATA4.json -X PUT http://127.0.0.1:8080/profile/$randomProfile -o /dev/null)
		 (echo "$1 : $requestCount : $result" >> results.log)
		 requestCount=$((requestCount+1)) 
(sleep $sleepTime)
	done
fi

# View profile's posts
if [ $2 == "view_profile_posts" ]; then
	while (true); do
		 randomProfile=$(shuf -i 1001-$lastProfile -n 1)
		 
		 result=$(curl -sL -w "%{http_code}" -X GET http://127.0.0.1:8080/profile/$randomProfile?posts -o /dev/null)
		 (echo "$1 : $requestCount : $result" >> results.log)
		 requestCount=$((requestCount+1)) 
(sleep $sleepTime)
	done
fi

# View profile's friend-list
if [ $2 == "view_friend_list" ]; then
	while (true); do
		 randomProfile=$(shuf -i 1001-$lastProfile -n 1)
		 
		 result=$(curl -sL -w "%{http_code}" -X GET http://127.0.0.1:8080/profile/$randomProfile?friendList -o /dev/null)
		 (echo "$1 : $requestCount : $result" >> results.log)
		 requestCount=$((requestCount+1)) 
(sleep $sleepTime)
	done
fi

## PAGE
# View page
if [ $2 == "view_page" ]; then
	while (true); do
		 randomPage=$(shuf -i 1001-$lastPage -n 1)
		 
		 result=$(curl -sL -w "%{http_code}" -X GET http://127.0.0.1:8080/page/$randomPage -o /dev/null)
		 (echo "$1 : $requestCount : $result" >> results.log)
		 requestCount=$((requestCount+1)) 
(sleep $sleepTime)
	done
fi

# Update page
if [ $2 == "update_page" ]; then	
	while (true); do
		 randomPage=$(shuf -i 1001-$lastPage -n 1)
		 
		 (cp -f ../../main/resources/files/pages/$randomPage.json DATA5.json)
		 (sed -i -e 's/\"additional_field5\": \"\"/\"additional_field5\": \"updated\"/g' DATA5.json)
		 
		 result=$(curl -sL -w "%{http_code}" -H "Content-Type: application/json" -d @DATA5.json -X PUT http://127.0.0.1:8080/page/$randomPage -o /dev/null)
		 (echo "$1 : $requestCount : $result" >> results.log)
		 requestCount=$((requestCount+1)) 
(sleep $sleepTime)
	done
fi

# View page's posts
if [ $2 == "view_page_posts" ]; then
	while (true); do
		 randomPage=$(shuf -i 1001-$lastPage -n 1)
		 
		 result=$(curl -sL -w "%{http_code}" -X GET http://127.0.0.1:8080/page/$randomPage?posts -o /dev/null)
		 (echo "$1 : $requestCount : $result" >> results.log)
		 requestCount=$((requestCount+1)) 
(sleep $sleepTime)
	done
fi
