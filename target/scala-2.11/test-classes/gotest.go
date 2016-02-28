package main

import (
	"fmt"
	"log"
	"os/exec"
	"strconv"
	"time"
)

func main() {
	
	new_post_own_count:= 20
	new_post_friend_count:= 27
	new_post_page_count:= 2
	view_profile_count:= 10
	view_profile_posts_count:= 10
	view_friend_list_count:= 10
	view_page_count:= 10
	view_page_posts_count:= 10
	timer:=0
	
	for i := 1; i <= new_post_own_count; i++  {
		go new_post_own(i)
	}
	for i := 1; i <= new_post_friend_count; i++  {
		go new_post_friend(i)
	}
	for i := 1; i <= new_post_page_count; i++  {
		go new_post_page(i)
	}
	for i := 1; i <= view_profile_count; i++  {
		go view_profile(i)
	}
	for i := 1; i <= view_profile_posts_count; i++  {
		go view_profile_posts(i)
	}
	for i := 1; i <= view_friend_list_count; i++  {
		go view_friend_list(i)
	}
	for i := 1; i <= view_page_count; i++  {
		go view_page(i)
	}
	for i := 1; i <= view_page_posts_count; i++  {
		go view_page_posts(i)
	}
	for {
		display_stats(timer)
		timer++
	}
	time.Sleep(1 * time.Minute)
}

func new_post_own(i int) {
	cmd := exec.Command("./test.sh", strconv.Itoa(i) ,"new_post_own")
	out,err := cmd.Output()
	if err != nil {
		log.Fatal(err)
	}
	fmt.Printf("%s\n",out)
}
func new_post_friend(i int) {
	cmd := exec.Command("./test.sh", strconv.Itoa(i) ,"new_post_friend")
	out,err := cmd.Output()
	if err != nil {
		log.Fatal(err)
	}
	fmt.Printf("%s\n",out)
}
func new_post_page(i int) {
	cmd := exec.Command("./test.sh", strconv.Itoa(i) ,"new_post_page")
	out,err := cmd.Output()
	if err != nil {
		log.Fatal(err)
	}
	fmt.Printf("%s\n",out)
}
func view_profile(i int) {
	cmd := exec.Command("./test.sh", strconv.Itoa(i) ,"view_profile")
	out,err := cmd.Output()
	if err != nil {
		log.Fatal(err)
	}
	fmt.Printf("%s\n",out)
}
func view_profile_posts(i int) {
	cmd := exec.Command("./test.sh", strconv.Itoa(i) ,"view_profile_posts")
	out,err := cmd.Output()
	if err != nil {
		log.Fatal(err)
	}
	fmt.Printf("%s\n",out)
}
func view_friend_list(i int) {
	cmd := exec.Command("./test.sh", strconv.Itoa(i) ,"view_friend_list")
	out,err := cmd.Output()
	if err != nil {
		log.Fatal(err)
	}
	fmt.Printf("%s\n",out)
}
func view_page(i int) {
	cmd := exec.Command("./test.sh", strconv.Itoa(i) ,"view_page")
	out,err := cmd.Output()
	if err != nil {
		log.Fatal(err)
	}
	fmt.Printf("%s\n",out)
}
func view_page_posts(i int) {
	cmd := exec.Command("./test.sh", strconv.Itoa(i) ,"view_page_posts")
	out,err := cmd.Output()
	if err != nil {
		log.Fatal(err)
	}
	fmt.Printf("%s\n",out)
}

func display_stats(timer int) {
		cmd := exec.Command("./statistics.sh")
		
		out,err := cmd.Output()
		if err != nil {
			log.Fatal(err)
		}
		fmt.Printf("Requests addressed: %s | Time: %d seconds\n",out,timer)
		
		sleep_cmd := exec.Command("sleep","1")
		_,sleep_err := sleep_cmd.Output()
		if sleep_err != nil {
			log.Fatal(sleep_err)
		}
}
