(ns exoscale.net.http.client.enum
  (:require [qbits.commons.enum :as enum])
  (:import (java.net.http HttpClient$Version
                          HttpClient$Redirect)))

(def version (enum/enum->fn HttpClient$Version))
(enum/defspec :exoscale.net.http.client/version HttpClient$Version)

(def redirect (enum/enum->fn HttpClient$Redirect))
(enum/defspec :exoscale.net.http.client/redirect HttpClient$Redirect)
