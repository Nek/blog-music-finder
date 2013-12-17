(ns music-finder.core
  (:require [cheshire.core :refer :all]
            [org.httpkit.client :as http]
            [org.httpkit.server :as serv]
            [monger.core :as mg])
  (:import [com.mongodb MongoOptions ServerAddress]))
(use '[clojure.java.shell :only [sh]])

(def client_id "a454bbc3e7c73dd307130edc1e2bed00")

(defn mk_request_url [name] (str "https://api.soundcloud.com/users/" name  "/tracks.json?client_id=" client_id))

(defn make_player [id] (str "<iframe width=\"100%\" height=\"166\" scrolling=\"no\" frameborder=\"no\" src=\"https://w.soundcloud.com/player/?url=https%3A//api.soundcloud.com/tracks/" id  "&amp;color=ff6600&amp;auto_play=false&amp;show_artwork=true\"></iframe>"))

(def ids (let [names ["warp-records" "stonesthrow"]]
    (->>
     (map mk_request_url names)
     (map http/get)
     (map (fn [req] (:body @req)))
     (map  #(parse-string % true))
     (map (fn [t] ( filter #(< (:duration %) 1200000) t)))
     (map (fn [t] ( sort #(not ( compare (:created_at %1) (:created_at %2))) t)))
     (map #(take 10 %))
     (mapcat  #(if (sequential? %) % [%])) ;; one level flatten
     (map :id)
     )))

(defn app [req]
  {:status  200
   :headers {"Content-Type" "text/html"}
   :body    (str  "<!DOCTYPE! html><http><head></head><body>" (reduce str (map make_player ids)) "</body></http>")})

(def stop-server (serv/run-server app {:port 8080}))

(stop-server)

;(mg/connect!)
;(mg/set-db! (mg/get-db "monger-test"))



                                        ;(:download_count :original_format :video_url :license :duration :track_type :kind :comment_count :state :sharing :label_id :attachments_uri :bpm :user_id :original_content_size :downloadable :isrc :purchase_url :stream_url :streamable :release_day :key_signature :title :playback_count :uri :permalink :created_at :waveform_url :genre :embeddable_by :release :commentable :label_name :user :favoritings_count :tag_list :release_month :release_year :id :permalink_url :description :purchase_title :artwork_url)
