(ns sculpt.generate
  "Pure candidate builder for one co-scientist round (ADR-2607123000 §2/§3).

  Same 'closed hypothesis pool, no LLM in Generation' discipline
  cloud_murakumo.cosci uses: a small enumerable gene pool of
  subject/category/scale variations, persona-flavored via `:persona/tags`.
  round-candidates is a pure function of (persona, round, k) — re-running the
  same round number reproduces the same candidates; exploration across the
  pool happens by round number advancing (sculpt.loop) and by biasing one
  gene slot toward the previous round's elite (sculpt.cosci/evolve-round)."
  (:require [clojure.string :as str]))

(def gene-pool
  {:subject ["a wooden market crate" "a roadside shrine post"
             "a folding camp stool" "a bicycle leaned on a wall"
             "a stack of ceramic bowls" "a wayfinding signpost"
             "a park bench" "a small fishing boat"]
   :category ["hard-surface prop" "organic prop" "stylized furniture"
              "environment set-dressing"]
   :scale ["hand-prop scale" "furniture scale" "vehicle scale"]})

(defn- pick [xs seed n] (nth xs (mod (+ seed n) (count xs))))

(defn- gene-for
  "One candidate's gene map. `bias` (from sculpt.cosci/evolve-round's elite,
  or nil on round 0) pins ONE randomly-chosen slot to the prior winner's
  value instead of round-robining it — elitism without literal crossover
  machinery, honest about being a small closed pool rather than a genuine
  genetic search."
  [round i bias]
  (let [raw {:subject  (pick (:subject gene-pool) round i)
             :category (pick (:category gene-pool) round (+ i 1))
             :scale    (pick (:scale gene-pool) round (+ i 2))}]
    (if (and bias (pos? round) (zero? (mod (+ round i) 3)))
      (merge raw (select-keys bias [(nth [:subject :category :scale] (mod round 3))]))
      raw)))

(defn round-candidates
  "persona + round n (0-based) + k candidates + optional elite bias
  -> [{:candidate/id :prompt :gene :params} ...]."
  ([persona n k] (round-candidates persona n k nil))
  ([{:keys [tags]} n k bias]
   (vec
    (for [i (range k)]
      (let [{:keys [subject category scale]} (gene-for n i bias)]
        {:candidate/id (str "r" n "-c" i)
         :prompt (str/join ", " (concat [subject category (str scale " scale")]
                                         tags))
         :gene {:subject subject :category category :scale scale}
         :params {}})))))
