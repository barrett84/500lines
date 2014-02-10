(ns core.fdb)

(defrecord Entity [e_id name attrs])
(defrecord Attr [name type value ts prev-ts])


(defn make-entity [name] (Entity.  :no-id-yet name {}))
(defn make-attr[name val type]  (Attr. name type val -1 -1))
(defn add-attr[ ent attr] (assoc-in ent [:attrs (keyword (:name attr))] attr))
(defn next-ts [db] (inc (:curr-time db)))


(defn update-creation-ts [ent tsVal]
  (let [ks (keys (:attrs ent))
        vls (vals (:attrs ent))
        updatedAttrsVals (map #(assoc % :ts tsVal) vls)
        updatedAttrs (interleave ks updatedAttrsVals)
        ]
        (assoc ent :attrs updatedAttrs)
    )

  )



;  AEVT -> {REFed-ent-id -> {attrName -> [REFing-elems-ids]}}
; this basically provides the info - for each entity that is REFFed by others, who are the others who are REFing it, separated
; by the names of the attribute used for reffing


(defn add-ref-to-aevt[ent aevt attr]
  (let [
        reffed-id (:value attr)
        attr-name (:name attr)
        reffed-ent-map (aevt reffed-id)
        reffed-ent-map (if reffed-ent-map reffed-ent-map {reffed-id {attr-name []}})
        reffing-ents-vec (attr-name reffed-ent-map)
        reffing-ents-vec (if reffing-ents-vec reffing-ents-vec [])
       ]
         (assoc aevt reffed-id (assoc reffed-ent-map attr-name (conj reffing-ents-vec (:e_id ent))))
     )
)

(defn update-aevt[old-aevt ent]
  (let [reffingAttrs (filter #(= :REF (:type %)) (vals (:attrs ent)))
        add-ref (partial add-ref-to-aevt ent)]
       (reduce add-ref old-aevt reffingAttrs))
  )

;when adding an entity, its attributes' timestamp would be set to be the current one
(defn add-entity[db ent]   (let [

                                 ent-id (inc (:topId db))
                                 new-ts (next-ts db)
                                 ts-mp (last (:timestamped db))
                                 fixed-ent (assoc ent :e_id ent-id)
                                 new-eavt (assoc (:EAVT ts-mp) ent-id  (update-creation-ts fixed-ent new-ts) )
                                 old-aevt (:AEVT ts-mp)
                                 new-aevt (update-aevt old-aevt fixed-ent)
                                 new-indices (assoc ts-mp :AEVT new-aevt :EAVT new-eavt )]
                                (assoc db
                                  :timestamped  (conj (:timestamped db) new-indices)
                                  :curr-time new-ts
                                  :topId ent-id)
                             ))

(defn make-db[]
  (atom {:timestamped [{:EAVT {} ; all the entity info
                        :AEVT {} ; for attrs who are REFs, we hold the back-pointing (from the REFFed entity to the REFing entities)
                        }]
                  :topId 0
                  :curr-time 0
                  }
    )
  )



(def db1 (make-db))

(def en1 (-> (make-entity "hotel")

          (add-attr (make-attr :hotel/room 12 :number))
          (add-attr (make-attr :hotel/address "where" :string)))

  )

(def en2 (-> (make-entity "book")

          (add-attr (make-attr :book/found-at 1 :REF))
          (add-attr (make-attr :book/author "jon" :string)))

  )

(def en3 (-> (make-entity "gate")

          (add-attr (make-attr :book/found-at 1 :REF))
          (add-attr (make-attr :gate/color "black" :string)))

  )

(swap! db1 add-entity en1)

(swap! db1 add-entity en2)


(swap! db1 add-entity en3)

(:AEVT (last (:timestamped @db1)))

;(defn recent-ts-val [db](last (:timestamped db)))


;(defn update-ts-with-EAV [ts &[e a v] :as more]
;  (let[eavt (:EAVT ts)]
;    (assoc-in ts :EAVT e a v)
;    )
;  )

;(defn _add-timestamp[db & more]
;  (let [ ts {:EAVT (:EAVT  (recent-ts-val db))}]
;    (update-ts-with-EAV ts more)
;
;    (assoc db :timestamped (conj (:timestamped db) ts))
;    )
; )




   ;(add-attr @db1 "name" :string "Jim" )
 ;  (add-attr  @db1 "sur-name" :string "Doe" )
 ;  (add-attr  @db1 "age-name" :number 39 )


;db
;; (defn fact[n]
;;   (if (<= n 0) 1
;;     (* n (fact (dec n)))
;;     )

;;   )

;; (fact 1)

;(swap! db assoc :6 5
