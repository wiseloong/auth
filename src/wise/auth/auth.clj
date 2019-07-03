(ns wise.auth.auth
    (:require [clj-time.core :as time]
      [clj-http.client :as client]
      [cheshire.core :as cjson]
      [buddy.auth.backends.token :refer [jws-backend]]
      [buddy.sign.jwt :as jwt]))

(def ^:private secret "muyun-ba")

(def auth-backend (jws-backend {:secret secret}))

(defn sign-token [claims]
      (-> {:user claims}
          (merge {:exp (time/plus (time/now) (time/seconds 3600))})
          (jwt/sign secret)))

(defn unsign-token [token]
      (-> token
          (jwt/unsign secret)))

(defn- post-access-token "根据获取的code，post请求获取accessToken"
       [uri registration code]
       (-> uri
           (client/post
             {:query-params {:grant_type    "authorization_code"
                             :response_type "code"
                             :client_id     (:client-id registration)
                             :client_secret (:client-secret registration)
                             :redirect_uri  (:redirect-uri registration)
                             :code          code}})
           :body))

(defn- get-user-info "根据获取的accessToken，get请求获取用户信息"
       [uri access-token]
       (-> (client/get (str uri "?" access-token))
           :body))

(defn oauth-wise-user "oauth方式获取用户信息"
      [data]
      (when-let [code (:code data)]
                (let [provider (:provider data)
                      registration (:registration data)
                      access-token (post-access-token (:token-uri provider) registration code)
                      user-info (get-user-info (:user-info-uri provider) access-token)]
                     (cjson/parse-string user-info true))))
