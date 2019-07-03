(ns wise.auth.login
  (:require [reagent.core :as r]
            [reagent.cookies :as cookies]
            [ajax.core :refer [POST]]
            [clojure.string :as cstr]
            [goog.string :as gstr]
            [goog.uri.utils :as guu]))

(def ^:private login-input (r/atom {:username nil :password nil}))

(defn- wis-ul [li-list]
       [:ul.list-group
        (for [li li-list]
             ^{:key li}
             [:li.list-group-item li])])

(defn- wis-img [src class-name]
       [:img {:class class-name :src src}])

(defn- input-login
       [vdata keys tips type i-name i-class]
       [:div.login-item
        [:i {:id i-name :class i-class}]
        [:input.form-control
         {:type        type
          :placeholder tips
          :on-change   (fn [e]
                           (swap! vdata assoc-in keys (-> (.-target e) (.-value))))
          :on-focus    (fn [e]
                           (.addClass (js/$ (str "#" i-name)) "active"))
          :on-blur     (fn [e]
                           (.removeClass (js/$ (str "#" i-name)) "active"))}]])

(defn- wis-login [handler]
       [:div.card
        [:div.card-header.text-center
         [:img.img-header {:src "img/header.png"}]
         [:div.login-title "账号登录"]]
        [:div.card-block
         [wis-ul
          [[input-login login-input [:username]
            "请输入账号" "text" "user" "iconfont icon-wode-active"]
           [input-login login-input [:password]
            "请输入密码" "password" "password" "iconfont icon-mima"]
           [:div.rember-password
            [:label.checkbox-inline
             [:input {:type "checkbox" :default-checked true}] "记住密码"]]
           [:button.btn.btn-primary.btn-block
            {:on-click #(handler login-input)}
            "登录"]]]]])

(defn- differ-minute [date]
       (let [now (js/Date.now)
             differ (- now date)]
            (/ differ 60000)))

(defn- href "跳转url，同时可用于按钮下载"
       [url] (set! (.-href js/location) url))

(defn- check-uri [uri]
       (if (cstr/ends-with? uri "/")
         uri
         (str uri "/")))

(defn- url-encode [s]
       (gstr/urlEncode s))

(defn- url-param-value
       ([k] (url-param-value (.-href js/location) k))
       ([url k] (guu/getParamValue url k)))

(defn- merge-uri [m]
       (if-let [uri (:uri m)]
               (reduce (fn [p [k v]]
                           (if (cstr/ends-with? (name k) "-uri")
                             (into p {k (str uri v)})
                             (into p {k v})))
                       {}
                       m)
               m))

(defn- check-login? "刷新页面时判断是否需要登陆" []
       (let [token (cookies/get :token)
             create-date (cookies/get :create-date)
             modify-date (cookies/get :modify-date)]
            (if-not (empty? token)
                    (let [c-d (differ-minute create-date)
                          m-d (differ-minute modify-date)]
                         (and (> 30 m-d) (> 55 c-d))))))

(def login-state (r/atom (check-login?)))

(defn- set-token [token]
       (cookies/set! :token token)
       (cookies/set! :create-date (js/Date.now))
       (cookies/set! :modify-date (js/Date.now)))

(defn- set-login "登陆完成" [data]
       (set-token (:token data))
       (reset! login-state true)
       (href "/#/"))

(defn- clear-login []
       (cookies/clear!)
       (reset! login-state false))

(defn- ajax-login [uri params f1 f2]
       (POST uri
             {:params        params
              :handler       #(do (f1 %) (set-login %))
              :error-handler f2}))

(defn- login-uri [config]
       (if (string? config)
         (if (cstr/index-of config "login")
           config (str config "auth/login"))
         (let [service (:service config)]
              (if (string? service)
                (str service "auth/login")
                (let [service (merge-uri service)]
                     (if-let [uri (:login-uri service)]
                             uri (str (:uri service) "auth/login")))))))

(defn- login-handler [uri f1 f2]
       (if (some nil? (vals @login-input))
         (js/alert "请输入账号和密码")
         (ajax-login uri @login-input f1 f2)))

(defn page-login [config init-f f1 f2]
      (let [uri (login-uri config)]
           (fn []
               (init-f)
               [:div.login
                [wis-img "img/logo.png" "login-header"]
                [:div.login-body [wis-login #(login-handler uri f1 f2)]]])))

(defn logout [f]
      (clear-login)
      (f))

;; 刷新认证
(defn- ajax-refresh [uri]
       (POST uri
             {:headers       {:authorization (str "Token " (cookies/get :token))}
              :handler       set-token
              :error-handler clear-login}))

(defn- refresh-uri [config]
       (if (string? config)
         (if (cstr/index-of config "refresh")
           config (str config "auth/refresh-token"))
         (let [service (:service config)]
              (if (string? service)
                (str service "auth/refresh-token")
                (let [service (merge-uri service)]
                     (if-let [uri (:refresh-uri service)]
                             uri (str (:uri service) "auth/refresh-token")))))))

(defn refresh-login [config]
      (let [uri (refresh-uri config)
            token (cookies/get :token)
            create-date (cookies/get :create-date)
            modify-date (cookies/get :modify-date)]
           (if-not (empty? token)
                   (let [c-d (differ-minute create-date)
                         m-d (differ-minute modify-date)]
                        (cond
                          (and (> 30 m-d) (< 50 c-d) (> 60 c-d)) (ajax-refresh uri)
                          (< 30 m-d) (clear-login))))))

;; oauth认证
(defn- ajax-oauth-login [uri params f1 f2]
       (POST uri
             {:params        params
              :handler       #(do (f1 %) (set-login %))
              :error-handler f2}))

(defn- oauth-login-uri [config]
       (let [service (:service config)]
            (if (string? service)
              (str service "auth/oauth-login")
              (let [service (merge-uri service)]
                   (if-let [uri (:oauth-login-uri service)]
                           uri (str (:uri service) "auth/oauth-login"))))))

(defn- oauth-config [config]
       (let [oauth2 (:oauth2 config)
             provider (-> oauth2 :provider merge-uri)
             registration (:registration oauth2)
             redirect-uri (-> registration :redirect-uri (str "#/oauth"))
             registration (assoc registration :redirect-uri redirect-uri)]
            {:provider provider :registration registration}))

(defn- authorize-uri [oauth]
       (let [provider (:provider oauth)
             registration (:registration oauth)
             authorize-uri (:authorization-uri provider)
             client-id (:client-id registration)
             redirect-uri (url-encode (:redirect-uri registration))]
            (str authorize-uri "?response_type=code&scope=simple&client_id=" client-id "&redirect_uri=" redirect-uri)))

(defn- oauth-login [path config init-f f1 f2]
       (let [oauth-config (oauth-config config)]
            (if (cstr/index-of path "/oauth?")
              (let [uri (oauth-login-uri config)
                    code (url-param-value path "code")]
                   (init-f)
                   (when code
                         (ajax-oauth-login uri (merge oauth-config {:code code}) f1 f2)
                         nil))
              (let [authorize-uri (authorize-uri oauth-config)]
                   (href authorize-uri)
                   nil))))

(defn- login* [path config init-f f1 f2]
       (fn []
           (if (and (contains? (:login-type config) :base) (cstr/index-of path "/login"))
             [page-login config init-f f1 f2]
             (oauth-login path config init-f f1 f2))))

(defn modify-auth "记录操作时间" []
      (cookies/set! :modify-date (js/Date.now)))

(defn login [path config init-f f1 f2]
      (fn []
          (if (contains? (:login-type config) :oauth2)
            (login* path config init-f f1 f2)
            [page-login config init-f f1 f2])))

(defn oauth-logout [config]
      (let [oauth-config (:oauth2 config)
            logout-uri (-> oauth-config :provider merge-uri :logout-uri)
            logout-redirect-uri (-> oauth-config :registration :redirect-uri (str "#/logout"))]
           (href (str logout-uri "?service=" (url-encode logout-redirect-uri)))))

(defn oauth-logout-plan [path clear-fn]
      (when (cstr/index-of path "/logout")
            (clear-login)
            (clear-fn)))
