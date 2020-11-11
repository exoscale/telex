(ns exoscale.telex.enum
  (:require [qbits.commons.enum :as enum])
  (:import (java.net.http HttpClient$Version
                          HttpClient$Redirect)))

(def version (enum/enum->fn HttpClient$Version))
(enum/defspec :exoscale.telex/version HttpClient$Version)

(def redirect (enum/enum->fn HttpClient$Redirect))
(enum/defspec :exoscale.telex/redirect HttpClient$Redirect)
