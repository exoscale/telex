(ns exoscale.net.http.client.enum
  (:require [qbits.commons.enum :as enum])
  (:import (java.net.http HttpClient$Version
                          HttpClient$Redirect)))

(def version (enum/enum->fn HttpClient$Version))
(def redirect (enum/enum->fn HttpClient$Redirect))
