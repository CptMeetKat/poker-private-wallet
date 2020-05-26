(ns poker.discord.display
  (:require [poker.logic.pots :as pots]
            [clojure.string :as strings]))

(def ^:private black-ranks
  {:ace   623575870375985162
   :two   623564440574623774
   :three 623564440545263626
   :four  623564440624824320
   :five  623564440851316760
   :six   623564440679350319
   :seven 623564440754978843
   :eight 623564440826150912
   :nine  623564440868225025
   :ten   623564440620630057
   :jack  623564440951980084
   :queen 623564440851185679
   :king  623564440880807956})

(def ^:private red-ranks
  {:ace   623575868672835584
   :two   623564440989859851
   :three 623564440880545798
   :four  623564441103106058
   :five  623564440868225035
   :six   623564440759173121
   :seven 623564440964694036
   :eight 623564440901779496
   :nine  623564440897454081
   :ten   623564440863899663
   :jack  623564440582881282
   :queen 623564440880807936
   :king  623564441073614848})

(defn- with-suit [rank-map suit]
  (reduce-kv (fn [acc rank id]
               (assoc acc (conj [suit] rank) id))
             {}
             rank-map))

(def upper-halves
  (merge (with-suit black-ranks :clubs)
         (with-suit black-ranks :spades)
         (with-suit red-ranks :hearts)
         (with-suit red-ranks :diamonds)
         {[nil nil] 714565166070759454}))

(def lower-halves
  {:clubs    623564441224740866
   :spades   623564441094586378
   :hearts   623564441065226267
   :diamonds 623564440926683148
   nil       714565093798576455})

(defn- emote-mention [id]
  (str "<:_:" id ">"))

(defn- halves-str [cards upper]
  (let [keyfn (if upper (juxt :suit :rank) :suit)
        halves-map (if upper upper-halves lower-halves)]
    (strings/join
      " "
      (->> cards
           (map keyfn)
           (map halves-map)
           (map emote-mention)))))

(defn cards->str
  ([cards fill-to]
   (let [cards (concat cards (repeat (- fill-to (count cards)) nil))]
     (str
       (halves-str cards true)
       "\n"
       (halves-str cards false))))
  ([cards] (cards->str cards 0)))

(defn user-mention [id]
  (str "<@" id ">"))

(defn pots->str [[{:keys [money]} & side-pots]]
  (str
    "**Main pot:** `" money "` chips\n"
    (if side-pots
      (strings/join "\n" (map-indexed #(str "**Side pot " (inc %1) ":** `" (:money %2) "` chips") side-pots))
      "")))

(def action->emoji
  {:fold   "\uD83C\uDDEB"
   :all-in "\uD83C\uDDE6"
   :check  "\uD83C\uDDE8"
   :call   "\uD83C\uDDE8"
   :raise  "\uD83C\uDDF7"})

(def emoji->action
  (zipmap (vals action->emoji) (keys action->emoji)))

(defn- move->str [{:keys [action cost]}]
  (str (action->emoji action) " " (strings/capitalize (name action)) " - `" cost "` chips"))

(defn moves->str [moves]
  (strings/join "\n" (map move->str moves)))

(defn turn->str
  [{[player-id] :cycle :keys [community-cards budgets] :as game}]
  (str
    "**Community Cards:**\n"
    (cards->str community-cards 5) "\n"
    (pots->str (:pots (pots/flush-bets game))) "\n\n"
    "It's your turn, " (user-mention player-id) "!\n"
    "What would you like to do? You still have `" (budgets player-id) "` chips.\n"))

(defn player-notification
  [{:keys [players player-cards budgets]} player-id]
  (str
    "Hi " (user-mention player-id) ", here are your cards for this game:\n"
    (cards->str (player-cards player-id)) "\n"
    "You have a budget of `" (budgets player-id) "` chips.\n"
    "We're playing No-Limit Hold'em. You can read up on the rules here:
    <https://en.wikipedia.org/wiki/Texas_hold_%27em>

    Your opponents are: " (strings/join ", " (map user-mention (disj players player-id))) ".\n"
    "**Have fun!**"))