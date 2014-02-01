; Most of the commands of Light Table are accessible from menus
; but there is at least one shortcut you have to remember.
; ctrl + enter to evaluate expression.
; Put the cursor on the (+ 2 2) line and press the shortcut.
; Wait for some time while Light Table sets up the environment.
; Then you'll see 4 as a result.
(+ 2 2)

; Lisp crash course
; Evaluate each line

; Call built-in functions
(+ 1 2)
(* 2 -5)

; Define a variable
(def a 5)

; Define a function
(defn plus [x y] (+ x y))

; Call your own function
(plus 3 4)

; Combine variable definition and function call
(def sum (plus 1 -6))

; Evaluate variable name to see what's inside
sum

; Those lines in parentheses are called expressions.

; Evaluate the following expression.
; You'll see activity indicator at the bottom of this window.
; Wait for it to disappear.
(ns music-finder.core
  (:require [cheshire.core :refer :all]
            [org.httpkit.client :as http]
            [org.httpkit.server :as serv]))

; Now we have a preset environment with some tools for the job.
; I'll tell you about each tool at the relevant step of the tutorial.

; Another useful shortcut is ctrl + shift + d
; It opens documentation search.
; The search works for imported libraries.
; Try to enter httpkit.client
; Hit esc to close the docs pane.

; Time for some real action!
; To access SounClouds APi public app key is needed.
; I've provided my own key for your convinience
; but if you are going to develop your own app
; you have to use your own key.

(def client-id "a454bbc3e7c73dd307130edc1e2bed00")

; Let's get tracks from one user.
; The relevant API method desciption is here
; http://developers.soundcloud.com/docs/api/reference#tracks
; First of all we have to form the right url to get tracks data from:

(defn mk-request-url [name]
  (str "https://api.soundcloud.com/users/" name  "/tracks.json?client_id=" client-id))

; The next line should give you
; "https://api.soundcloud.com/users/ninja-tune/tracks.json?client_id=a454bbc3e7c73dd307130edc1e2bed00"
; You'll probably have to click on it to unfold.

(mk-request-url "ninja-tune")

; It's time to use one of the tools.
; Previously we required org.httpkit.client with shortname of http.
; It has get method (use ctrl + shift + d to find the docs on it).
; I assume you have a working internet connection :)

(def req (http/get (mk-request-url "warp-records")))

; Now we have a "req" but what is it?

req

; It's something called #<core$promise...
; You can think of a promise is a container for an asynchronous action
; To get what's inside you prefix the promise with @ symbol.
; That's called dereffing.
; You'll have the result immediately if it's already there.
; If it's pending your code'll block until it is delivered.

; You have to be online for the next line to work.
; The action takes some time so you'll be seeing activity indicator again.

@req

; Let's see the data.

(:body @req)

; It's a string. That's not convenient. We'll deal with it later.

; If you are offline the result'll be different.
; Let's check it. Go offline and evaluate next four expressions one by one.

(def req2 (http/get (mk-request-url "warp-records")))
@req2
(:body @req2)
(:error @req2)

; As you can see the :body has nothing and there is an :error

; But why don't we just use a callback?
; Bear with me those promises'll come in handy
; when we'll be dealing with a bunch of requests.

; Let's get back to the task. Go back online.
; Time to use another tool to parse the data string into a data structure.
; parse-string function from the cheshire library lets you do it.

(def tracks (parse-string (:body @req)))
tracks
(count tracks)

; Now we have a list of tracks. That looks better.
; Let's see what kinds of information about a track are available:

(keys (first tracks))

; First one is "license". Let's try to see it's value.

("license" (first tracks))

; Oh no! That's an error.
; We can't read the data from a map by a string.
; Let's fix it.

(def tracks (parse-string (:body @req) true))
tracks

; Now it's a proper key value map.

(:license (first tracks))

; Time to get tracks from several labels using map

(def tracks
  (map (fn [t] @t)
       (map http/get
            (map mk-request-url ["ninja-tune" "stonesthrow" "warp-records"]))))

tracks

; This works but the nested code looks ugly.
; This style of programming can get out of hand fast.
; Clojure awesomeness to the rescue!
; The ->> form lets you chain function calls.
; Each subsequent function will be called
; with the result of the previous function

(def tracks
  (->>
   (map mk-request-url ["ninja-tune" "stonesthrow" "warp-records"])
   (map http/get)
   (map (fn [r] @r))))

tracks

; Looks much better!
; Now we can build on top of this.
; Time to add back data parsing.

(def tracks
  (->>
   (map mk-request-url ["ninja-tune" "stonesthrow" "warp-records"])
   (map http/get)
   (map (fn [r] (:body @r)))
   (map (fn [d] (parse-string d true)))
   ))

tracks

; You've probably noticed that we have a list of lists at the moment.
; That's not what we want. Let's flatten it.

(def tracks
  (->>
   (map mk-request-url ["ninja-tune" "stonesthrow" "warp-records"])
   (map http/get)
   (map (fn [r] (:body @r)))
   (map (fn [d] (parse-string d true)))
   (mapcat identity)
   ))

tracks

; Much better! It's just a list of tracks now.
; Let's see what kinds of data we can use to sort the tracks.

(keys (first tracks))

; Those fields look interesting:
; :duration
; :created_at
; :favoritings_count
; :playback_count

; Some tracks are actually live sets or radio programs.
; I'm pretty sure we can ignore everything longer than 20 minutes.
; Let's use :duration to make it happen.

(def tracks
  (->>
   (map mk-request-url ["ninja-tune" "stonesthrow" "warp-records"])
   (map http/get)
   (map (fn [r] (:body @r)))
   (map (fn [d] (parse-string d true)))
   (mapcat identity)
   (filter (fn [t] (< (:duration t) (* 20 60 1000))))
   ))

tracks

; Let's find ten latest tracks

(def ten-latest-tracks
  (->>
   (sort-by :playback_count tracks)
   (reverse) ; yes we need this
   (take 10)))

ten-latest-tracks

; Or ten most favorited tracks

(def ten-most-favored-tracks
  (->>
   (sort-by :favoritings_count tracks)
   (reverse)
   (take 10)))

ten-most-favored-tracks

; Or ten most played tracks

(def ten-most-played-tracks
  (->>
   (sort-by :playback_count tracks)
   (reverse)
   (take 10)))

ten-most-played-tracks

; In the end it's not that hard.

; Time to use org.httpkit.server.

(def links-to-tracks (map :permalink_url ten-most-played-tracks))

(defn app [req]
  {:status  200
   :headers {"Content-Type" "text/html"}
   :body    (str  "<!DOCTYPE! html><html><head><script type=\"text/javascript\" src=\"http://ajax.googleapis.com/ajax/libs/jquery/1.7.2/jquery.min.js\"></script><script type=\"text/javascript\" src=\"http://stratus.sc/stratus.js\"></script></head><body><script type=\"text/javascript\">
  $(document).ready(function(){
    $.stratus({links: '"
                  (reduce #(str %1 "," %2) links-to-tracks)
                  "'});
  });
</script></body></html>")})

(def stop-server (serv/run-server app {:port 8080}))

; Open http://localhost:8080 in your browser.
; It'll take some time to load all ten tracks data.
; Enjoy the music!


; Evaluate the next expression to stop the server
(stop-server)
