(ns wise.auth.core
    (:require
      #?(:clj  [wise.auth.auth :as auth]
         :cljs [wise.auth.login :as login])))

;; 后端服务-------------------

#?(:clj
   (defn oauth-wise-user-id "oauth方式获取用户ID"
         [data]
         (-> data auth/oauth-wise-user :id)))

#?(:clj
   (defn sign-token "编译信息为token"
         [claims]
         (auth/sign-token claims)))

#?(:clj
   (defn unsign-token "解译token信息"
         [token]
         (auth/unsign-token token)))

;; 前端服务-------------------

;; 普通登陆的配置信息，只需要一个后台服务地址即可。
#_(def config {:service "http://localhost:3000/"})

#?(:cljs
   (defn refresh
         "刷新认证信息方法
         config     - 配置信息，普通登陆时可为刷新时调用后台接口地址，oauth认证登陆时，为oauth配置信息"
         [config]
         (login/refresh-login config)))

#?(:cljs
   (defn logout
         "普通退出登陆方法
         clear-fn   - 退出按钮的清理方法，比如退出后清除用户信息"
         [clear-fn]
         (login/logout clear-fn)))

#?(:cljs
   (defn login
         "普通登陆
         main       - 为登陆成功后的主页面
         config     - 配置信息，主要为登陆时调用后台接口地址
         init-fn    - 需要在登陆页面初始化方法，比如获取元数据信息
         success-fn - 登陆成功后执行的方法，接收参数为登陆方法返回值，比如登陆成功后修改用户信息
         error-fn   - 登陆失败后执行的方法，比如打印失败信息"
         [main config init-fn success-fn error-fn]
         (fn []
             (if @login/login-state
               [:div {:on-click login/modify-auth} main]
               [login/page-login config init-fn success-fn error-fn]))))

;; oauth配置信息示例：
;; config为oauth配置信息，其中:login-type包含:oauth2时主页采用oauth2认证，
;; 当包含:base时可以访问#/login地址，来采用普通登陆方式登陆，
;; 如果后台接口地址按照下面:service的配置地址，可以只给 :service "http://localhost:3000/" 即可。
#_(def config {:login-type #{:oauth2 :base}
               :oauth2     {:provider     {:uri               "http://192.168.17.26/"
                                           :authorization-uri "cas/oauth2.0/authorize"
                                           :token-uri         "cas/oauth2.0/accessToken"
                                           :user-info-uri     "cas/oauth2.0/profile"
                                           :logout-uri        "cas/logout"}
                            :registration {:provider      "wise-sso"
                                           :client-id     "localcastest"
                                           :client-secret "localcastest"
                                           :redirect-uri  "http://127.0.0.1:3449/"
                                           :scope         "simple"
                                           :client-name   "hrms"}}
               :service    {:uri             "http://localhost:3000/"
                            :login-uri       "auth/login"
                            :oauth-login-uri "auth/oauth-login"
                            :refresh-uri     "auth/refresh-token"}})
#?(:cljs
   (defn oauth-logout
         "智隆oauth认证，退出认证登陆方法
         config     - oauth配置信息"
         [config]
         (login/oauth-logout config)))

#?(:cljs
   (defn oauth-login
         "智隆oauth认证对接封装，需要认证中心2.8
         main       - 为登陆成功后的主页面
         config     - oauth配置信息
         init-fn    - 需要在登陆页面初始化方法，比如获取元数据信息
         clear-fn   - 退出按钮的清理方法，比如退出后清除用户信息
         success-fn - 登陆成功后执行的方法，接收参数为登陆方法返回值，比如登陆成功后修改用户信息
         error-fn   - 登陆失败后执行的方法，比如打印失败信息"
         [main path config init-fn clear-fn success-fn error-fn]
         (fn []
             (login/oauth-logout-plan path clear-fn)
             (if @login/login-state
               [:div {:on-click login/modify-auth} main]
               [login/login path config init-fn success-fn (fn [d] (error-fn d) (oauth-logout config))]))))
