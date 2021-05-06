package me.egaetan.slay;

import static guru.nidi.graphviz.attribute.Attributes.attr;
import static guru.nidi.graphviz.model.Factory.graph;
import static guru.nidi.graphviz.model.Factory.node;
import static guru.nidi.graphviz.model.Factory.to;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import guru.nidi.graphviz.attribute.Color;
import guru.nidi.graphviz.attribute.Font;
import guru.nidi.graphviz.attribute.Rank;
import guru.nidi.graphviz.attribute.Rank.RankDir;
import guru.nidi.graphviz.attribute.Rank.RankType;
import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
import guru.nidi.graphviz.model.Graph;
import guru.nidi.graphviz.model.LinkSource;
import guru.nidi.graphviz.rough.FillStyle;
import guru.nidi.graphviz.rough.Roughifyer;

public class Main {

	
	enum NodeType {
		MONSTER,
		ENIGME,
		SHOP,
		ELITE,
		BOSS,
		REPOS,
		COFFRE,
		END,
		START,
	}
	
	
	static String[] reposName = new String[] {"Repos\n du faible", "Repos\n sans peine", "Repos\n à l'ombre", "Repos\n sans\n cauchemard", "Repos\n pas éternel", "Repos", "Repos\n gratuit"};
	
	static class Node {
		String title;
		private int h;
		private int level;
		NodeType type;
		
		Supplier<List<Monster>> monsters;
		private Reward reward;
		
		public Node(String title, int level, int h) {
			this.title = title;
			this.h = h;
			this.type = NodeType.MONSTER;
		}

		public Node type(NodeType type) {
			this.type = type;
			return this;
		}

		public List<Monster> monsters() {
			return retrieveMonsters().get();
		}

		public Supplier<List<Monster>> retrieveMonsters() {
			return monsters;
		}

		public Supplier<List<ItemShop>> retrieveItems() {
			return reward.cartes;
		}
		
		public List<ItemShop> items() {
			return retrieveItems().get();
		}
		
		public void setMonsters(Supplier<List<Monster>> retrieveMonsters) {
			monsters = retrieveMonsters;
		}
		
		public void setReward(Reward reward) {
			this.reward = reward;
		}
		
	}
	

	
	final static Random random = new Random(16486744);
	static int random(int n) {
		return random.nextInt(n);
	}
	
	public static interface MonsterAttack {
		public void attack(Monster monster, World world);
		
	}
	
	public static interface MonsterStrategy {

		public void prepareTurn(World world, Monster monster);

		public void endTurn(World world, Monster monster);

		public MonsterBrain get(World world, Monster monster);
		
		public static MonsterStrategy simpleStrategy(List<MonsterBrain> brains) {
			return simpleStrategy(brains, 0);
		}

		public static MonsterStrategy simpleStrategy(List<MonsterBrain> brains, int start) {
			return andThenSimpleStrategy(Collections.emptyList(), brains, start);
		}

		public static MonsterStrategy prepareAndThensimpleStrategy(List<MonsterBrain> brainsPrepare, List<MonsterBrain> brains) {
			return andThenSimpleStrategy(brainsPrepare, brains, 0);
		}

		public static MonsterStrategy andThenSimpleStrategy(List<MonsterBrain> brainsPrepare, List<MonsterBrain> brains, int start) {
			return  new MonsterStrategy() {

				int state = 0;
				boolean inPrepare=true;

				@Override
				public void prepareTurn(World world, Monster monster) {
					if (state == brainsPrepare.size() && inPrepare) {
						inPrepare = false;
						state = start;
					}
					state = (state) % brains.size();
				}

				@Override
				public void endTurn(World world, Monster monster) {
					state ++;
				}

				@Override
				public MonsterBrain get(World world, Monster monster) {
					return inPrepare ? brainsPrepare.get(state) : brains.get(state);
				}
			};
		}

	}
	
	enum MonsterAttackType {
		ATTACK,
		BUFF,
		BLOCK,
		MENTAL,
		;
	}
	public static class MonsterBrain {
		
		MonsterAttack attack;
		MonsterAttackType type;
		private String display;
		
		
		public MonsterBrain(MonsterAttack attack, MonsterAttackType type) {
			super();
			this.attack = attack;
			this.type = type;
			this.display = type.name();
		}
		public MonsterBrain(MonsterAttack attack, MonsterAttackType type, String display) {
			super();
			this.attack = attack;
			this.type = type;
			this.display = display;
		}

		public void attack(Monster monster, World world) {
			attack.attack(monster, world);
		}
		
		public static MonsterBrain attack(MonsterAttack a, int intent) {
			return new MonsterBrain(a, MonsterAttackType.ATTACK, "" + intent);
		}
		public static MonsterBrain attack(int i) {
			return new MonsterBrain((m, w) -> m.attackHero(w, i), MonsterAttackType.ATTACK, "Attack " + i);
		}
		public static MonsterBrain buff(MonsterAttack a) {
			return new MonsterBrain(a, MonsterAttackType.BUFF);
		}
		public static MonsterBrain block(MonsterAttack a) {
			return new MonsterBrain(a, MonsterAttackType.BLOCK);
		}
		public static MonsterBrain mental(MonsterAttack a) {
			return new MonsterBrain(a, MonsterAttackType.MENTAL);
		}

		public String display() {
			return display;
		}
	}

	interface HiddenEffects extends Effects {
		default boolean display() {return false;};
	}

	interface Effects {
		default void onDeath(Monster m, World w) {}

		default void endTurn(Monster m, World w) {};

		default void startTurn(Monster m, World w) {};
		
		default String description() {return "";};

		default boolean display() {return true;};
	}

	public static class Monster extends Personnage {
		String name;
		
		int originPv;
		private MonsterStrategy brain;

		public Monster(String name, int pv, MonsterStrategy brain) {
			this.name = name;
			this.originPv = pv;
			this.pv = pv;
			this.brain = brain;
		}

		public void attackHero(World world, int a) {
			world.attackHero(this, (int) ((a + force) * (faiblesse > 0 ? 0.75 : 1.)));
		}

		public String name() {
			return name;
		}

		public void originPv(int pv) {
			this.originPv = pv;
			this.pv = pv;
		}

	}
	
	/**
	 * shuffle in place
	 */
	static <T> List<T> shuffle(List<T> liste) {
		for (int i = 0; i< liste.size() * 10; i++) {
			int a = random(liste.size());
			int b = random(liste.size());
			T tmp = liste.get(a);
			liste.set(a, liste.get(b));
			liste.set(b, tmp);
		}
		return liste;
	}



	public abstract static class Personnage {

		int pv;
		int force;
		int armure;
		int vulnerability;
		int faiblesse;

		List<Effects> effects = new ArrayList<>();

		public <T extends Personnage> T effect(Effects e) {
			effects.add(e);
			return (T) this;
		}

		public AttackResult attack(int a) {
			int amount = (int) (Math.max(0, a - armure) * (vulnerability > 0 ? 1.5 : 1.));
			int b = armure - Math.max(0, armure - a);
			this.armure -= b;
			this.pv -= amount;
			return new AttackResult(a, amount, b);
		}

		public abstract String name();

		public void armure(int amount, Personnage from, World w) {
			armure += amount;
			w.describe(new ActionBuff(from, this, List.of(new ActionEffect(ActionEffectType.ARMURE, amount))));
		}

		public void force(int f, World w) {
			force(f, this, w);
		}

		public void force(int f, Personnage from, World w) {
			force += f;
			w.describe(new ActionBuff(from, this, List.of(new ActionEffect(ActionEffectType.FORCE, f))));
		}

		public void faiblesse(int f, Personnage from, World w) {
			faiblesse += f;
			w.describe(new ActionBuff(from, this, List.of(new ActionEffect(ActionEffectType.FAIBLESSE, f))));
		}

		public void vulnerability(int f, Personnage from, World w) {
			vulnerability += f;
			w.describe(new ActionBuff(from, this, List.of(new ActionEffect(ActionEffectType.VULNERABILITY, f))));
		}
	}
	
	
	public static class Heros extends Personnage {
		
		int maxPv;
		int energy;
		int startEnergy = 3;
		int gold = 50;
		int handSize = 5;
		
		List<Carte<?>> deck = new ArrayList<>();
		List<Carte<?>> hand = new ArrayList<>();
		List<Carte<?>> played = new ArrayList<>();
		List<Carte<?>> retired = new ArrayList<>();

		public int attack(Monster m, int a) {
			return (int) ((a + force) * (faiblesse == 0 ? 1 : 0.75));
		}

		public void drawCard() {
			if (deck.isEmpty()) {
				deck.addAll(shuffle(played));
				played.clear();
			}
			if (deck.isEmpty()) {
				return;
			}
			if (hand.size() >= 12) {
				return;
			}
			Carte<?> newCard = deck.get(random(deck.size()));
			deck.remove(newCard);
			hand.add(newCard);			
		}

		public void winGold(int amount) {
			this.gold += amount;
		}

		public void resetDeck() {
			deck.addAll(played);
			deck.addAll(hand);
			deck.addAll(retired);
			hand.clear();
			played.clear();
			retired.clear();
			List<Carte<?>> tmp = new ArrayList<>(deck);
			deck.clear();
			tmp.forEach(c -> {if (!c.ephemere && !c.ethereal) deck.add(c);});
		}

		public void clearHand(World world) {
			for (Carte<?> carte : new ArrayList<>(hand)) {
				clearCard(carte, world);
			}
			hand.clear();
		}

		public void clearCard(Carte<?> carte, World world) {
			if (carte.type == CarteType.GAME) {
				return;
			}
			carte.action.discard(world);
			if (!carte.ethereal) {
				played.add(carte);
			}
			else {
				retired.add(carte);
			}
		}

		@Override
		public String name() {
			return "Hero";
		}
	}
	
	enum WorldState {
		Map,
		Fight,
		ChoseReward,
		Shop,
		ContextMonster,
		ContextDeck,
		ContextSolo,
		;

		void next(World world) {
			world.state = world.contextState;
		}
	}
	
	public static class World {
		
		private MapWorld map;
		private WorldDeck deck;
		private Heros hero;
		
		private Node position = null;
		
		private WorldState state;
		private WorldState contextState;
		private Runnable endPlayable = () -> {};

		private Carte<?> currentCard;
		private Playable<?> currentPlayable;
		private int cardsRemoved;
		
		List<Monster> monsters= new ArrayList<>();
		Map<Monster, MonsterBrain> intentions = new HashMap<>();
		boolean firstTurn = false;
		List<BiConsumer<Carte<?>, World>> beforePlay = new ArrayList<>();
		List<BiConsumer<Carte<?>, World>> afterPlay = new ArrayList<>();
		List<IntUnaryOperator> attackStack = new ArrayList<>();
		List<ItemShop> shopItems = new ArrayList<>();

		List<Action> describe = new ArrayList<>();
		
		public World(MapWorld map, Heros heros, WorldDeck deck) {
			this.map = map;
			this.deck = deck;
			this.position = map.start();
			this.hero = heros;
			this.state = WorldState.Map;
		}

		public boolean isEnded() {
			return hero.pv <= 0 || position.type == NodeType.END;
		}

		public boolean inFight() {
			return state == WorldState.Fight;
		}

		public boolean inMap() {
			return state == WorldState.Map;
		}

		public boolean inChooseReward() {
			return state == WorldState.ChoseReward;
		}
		
		public boolean inShop() {
			return state == WorldState.Shop;
		}

		public void moveTo(Node node) {
			position = node;
			if (position.type == NodeType.MONSTER) {
				monsters = new ArrayList<>(position.monsters());
				startFight();
				startTurn();
			}
			else if (position.type == NodeType.ELITE) {
				monsters = new ArrayList<>(position.monsters());
				startFight();
				startTurn();
			}
			else if (position.type == NodeType.SHOP) {
				shopItems = new ArrayList<>(position.items());
				startShop();
			}
			else if (position.type == NodeType.COFFRE) {
				shopItems = new ArrayList<>(position.items());
				startCoffre();
			}
			else if (position.type == NodeType.REPOS) {
				hero.pv = Math.min(hero.maxPv, hero.pv + (hero.maxPv / 3+2));
			}
		}

		public void startShop() {
			state = WorldState.Shop;
			endPlayable = () -> {};
		}
		
		public void startCoffre() {
			state = WorldState.ChoseReward;
			endPlayable = () -> World.this.state = WorldState.Map;
		}
		
		public void startChooseReward() {
			state = WorldState.ChoseReward;

			hero.winGold(random(25) + 25);
			shopItems = new ArrayList<>(position.items());
			
			endPlayable = () -> World.this.state = WorldState.Map;
		}
		
		
		public void playChooseItem(ItemShop item) {
			if (item.price(this) <= hero.gold) {
				hero.gold -= item.price(this);
				Playable<?> bought = item.buy(this);
				shopItems.remove(item);
				currentPlayable = bought;
				currentPlayable.type().handle(this);
				//endPlayable.run();
			}
		}

		public <T> void play(Context<T> context) {
			describe.clear();
			state.next(this);
			@SuppressWarnings("unchecked")
			Playable<T> playable = (Playable<T>) currentPlayable;
			boolean play = playable.play(this, context);
			if (play) {
				currentPlayable = null;
				endPlayable.run();
			}
		}
		
		private void attackHero(Personnage monster, int a) {
			AttackResult x = hero.attack(a);
			if (x.brokenArmure != 0) {
				describe(new ActionFight(monster, hero, List.of(
						new ActionEffect(ActionEffectType.ATTACK, a),
						new ActionEffect(ActionEffectType.ARMURE, -x.brokenArmure),
						new ActionEffect(ActionEffectType.INJURY, x.injury))
				));
			}
			else {
				describe(new ActionFight(monster, hero, List.of(
						new ActionEffect(ActionEffectType.ATTACK, a),
						new ActionEffect(ActionEffectType.INJURY, x.injury))
				));
			}
		}

		public void attackMonster(Monster m, int a) {
			int attack = hero.attack(m, a);
			for (IntUnaryOperator i : attackStack) {
				attack = i.applyAsInt(attack);
			}
			AttackResult x = m.attack(attack);
			if (x.brokenArmure != 0) {
				describe(new ActionFight(hero, m, List.of(
						new ActionEffect(ActionEffectType.ATTACK, a),
						new ActionEffect(ActionEffectType.ARMURE, -x.brokenArmure),
						new ActionEffect(ActionEffectType.INJURY, x.injury))
				));
			}
			else {
				describe(new ActionFight(hero, m, List.of(
						new ActionEffect(ActionEffectType.ATTACK, a),
						new ActionEffect(ActionEffectType.INJURY, x.injury))
				));
			}
		}

		public void startFight() {
			state = WorldState.Fight;
			shuffle(hero.deck);
			for (Carte<?> c : hero.deck) c.resetCost();
			intentions.clear();
			hero.vulnerability = 0;
			hero.faiblesse = 0;
			hero.armure = 0;
			hero.force = 0;
			endPlayable = this::afterPlay;
			beforePlay.clear();
			afterPlay.clear();
			firstTurn = true;
		}
		
		public void startTurn() {
			hero.energy = hero.startEnergy ;
			hero.vulnerability = Math.max(0, hero.vulnerability - 1);
			hero.faiblesse = Math.max(0, hero.faiblesse - 1);
			hero.armure = 0;
			
			hero.hand.add(deck.endTurn());
			
			for (int i = 0; i < hero.handSize + (firstTurn ? 2 : 0); i++) {
				hero.drawCard();
			}
			for (Monster m : monsters) {
				if (m.pv > 0) {
					m.brain.prepareTurn(this, m);
				}
			}
			firstTurn = false;
			monstersIntentions();
		}

		private void monstersIntentions() {
			intentions.clear();
			for (Monster m : monsters) {
				if (m.pv > 0) {
					intentions.put(m, m.brain.get(this, m));
				}
			}
		}

		public void endTurn() {
			describe.clear();
			hero.clearHand(this);

			for (Monster m : new ArrayList<>(monsters)) {
				if (m.pv > 0) {
					for (Effects e : new ArrayList<>(m.effects)) {
						e.startTurn(m, this);
					}
				}
			}
			for (Monster m : new ArrayList<>(monsters)) {
				if (m.pv > 0) {
					intentions.get(m).attack(m, this);
				}
			}
			for (Monster m : new ArrayList<>(monsters)) {
				if (m.pv > 0) {
					m.vulnerability = Math.max(0, m.vulnerability - 1);
					m.faiblesse = Math.max(0, m.faiblesse - 1);
					m.armure = 0;
					for (Effects e : new ArrayList<>(m.effects)) {
						e.endTurn(m, this);
					}
				}
			}
			for (Monster m : monsters) {
				if (m.pv > 0) {
					m.brain.endTurn(this, m);
				}
			}
			checkEndFight();
			if (inFight()) {
				startTurn();
			}
		}

		private void checkEndFight() {
			List<Monster> killed = new ArrayList<>();
			for (Monster m : new ArrayList<>(monsters)) {
				if (m.pv <= 0) {
					m.effects.forEach(e -> e.onDeath(m, this));
					describe(new ActionDeath(m));
					killed.add(m);
				}
			}
			monsters.removeAll(killed);
			
			if (monsters.isEmpty()) {
				endFight();
			}
		}

		private void endFight() {
			hero.resetDeck();
			beforePlay.clear();
			afterPlay.clear();
			
			startChooseReward();
		}

		public void playCard(Carte<?> carte) {
			if (hero.energy >= carte.cost && carte.type != CarteType.UNPLAYABLE && hero.hand.remove(carte)) {
				hero.energy -= carte.cost;
				
				hero.clearCard(carte, this);
				
				this.currentCard = carte;
				this.currentPlayable = carte.action;

				beforePlay(carte);
				currentPlayable.type().handle(this);

				monstersIntentions();
			}
		}

		private void beforePlay(Carte<?> carte) {
			for (var c : new ArrayList<>(beforePlay)) {
				c.accept(carte, this);
			}
		}

		private void afterPlay() {
			for (var c : new ArrayList<>(afterPlay)) {
				c.accept(this.currentCard, this);
			}
			this.currentCard = null;
			checkEndFight();
		}

		public void describe(Action action) {
			describe.add(action);
		}

		public void deck(Carte<?> carte, Personnage from) {
			hero.deck.add(carte);
			describe(new ActionBuff(from, hero, List.of(new CarteActionEffect(carte))));
		}

		public void monsters(Monster add, Personnage from) {
			describe(new ActionBuff(from, hero, List.of(new SpawnMonsterActionEffect(add))));
		}
	}
	
	public static interface Context<T> {
		T from(World w);
	}
	
	enum ContextType {
		SOLO(WorldState.ContextSolo),
		DECK(WorldState.ContextDeck),
		MONSTER(WorldState.ContextMonster),
		;

		private final WorldState state;

		ContextType(WorldState state) {
			this.state = state;
		}
		
		public void handle(World world) {
			world.contextState = world.state;
			world.state = state;
		}
		
	}
	
	public static interface Playable<T> {
		
		@SuppressWarnings("unchecked")
		default Playable<World> world() {
			return ((Playable<World>) this);
		}
		@SuppressWarnings("unchecked")
		default Playable<Carte<?>> deck() {
			return ((Playable<Carte<?>>) this);
		}
		@SuppressWarnings("unchecked")
		default Playable<Monster> monster() {
			return ((Playable<Monster>) this);
		}
		
		static Playable<World>  notPlayable(Consumer<World> onDiscard) {
			return new Playable<World>() {
				@Override
				public void discard(World world) {
					onDiscard.accept(world);
				}

				@Override
				public boolean play(World world, Context<World> context) {return true;}

				@Override
				public ContextType type() {
					return ContextType.SOLO;
				}
			};
		}

		/**
		 * Called when hand is dicarded
		 * @param world
		 */
		default void discard(World world) {};
		
		/**
		 * 
		 * @param world
		 * @param context
		 * @return true if playable is finished, false otherwise
		 */
		boolean play(World world, Context<T> context);
		ContextType type();
		
		static Playable<World> solo(Consumer<World> action) {
			return new Playable<World>() {
				@Override
				public boolean play(World world, Context<World> context) {
					action.accept(world);
					return true;
				}
				
				@Override
				public ContextType type() {
					return ContextType.SOLO;
				}
			};
		}
		
		static Playable<Monster> monster(BiConsumer<World, Monster> action) {
			return new Playable<Monster>() {
				@Override
				public boolean play(World world, Context<Monster> context) {
					action.accept(world, context.from(world));
					return true;
				}
				
				@Override
				public ContextType type() {
					return ContextType.MONSTER;
				}
			};
		}
		
		public static Playable<World> attackStack(Predicate<Carte<?>> condition, IntUnaryOperator multiplier) {
			return Playable.solo((w) -> {
				
				
				w.beforePlay.add(new BiConsumer<Carte<?>, World>() {
					@Override
					public void accept(Carte<?> c, World z) {
						if (condition.test(c)) {
							w.beforePlay.remove(this);
							w.attackStack.add(multiplier);

							w.afterPlay.add(new BiConsumer<Carte<?>, World>() {
								@Override
								public void accept(Carte<?> c, World z) {
									w.attackStack.remove(multiplier);
									w.afterPlay.remove(this);
								}
							});
						}
					}
				}); 
			});
		}


	}
	
	enum CarteType {
		ATTACK,
		ARMURE,
		POWER,
		UNPLAYABLE, 
		GAME,
		;
		
		Predicate<Carte<?>> is() {
			return c -> c.type == this; 
		}
	}
	
	
	static class Reward {
		int gold;
		Supplier<List<ItemShop>> cartes;
		
		public Reward(int gold, Supplier<List<ItemShop>> cartes) {
			super();
			this.gold = gold;
			this.cartes = cartes;
		}
	}
	
	public static interface ItemShop {
		
		public static ItemShop carte(int price, Carte<?> carte) {
			return new ItemShop() {

				@Override
				public Playable<?> buy(World world) {
					return Playable.solo(w -> w.deck(carte, w.hero));
				}

				@Override
				public int price(World __) {
					return price;
				}

				@Override
				public Carte<?> description() {
					return carte;
				}
				
			};
			
		}
		
		public Playable<?> buy(World world);

		public int price(World world);
		
		public Carte<?> description();
		
	}
	
	public static class Carte<T> {
		int id;

		CarteClass classe;
		int cost;
		int origineCost;

		// Disappear at the end of the fight
		boolean ephemere = false;

		// Disappear at the end of the turn
		boolean ethereal = false;

		String name;
		Playable<T> action;
		CarteType type;
		
		public Carte(int id, CarteType type, int originEnergy, CarteClass classe, String name, Playable<T> action) {
			super();
			this.id = id;
			this.origineCost = originEnergy;
			this.cost = originEnergy;
			this.classe = classe;
			this.name = name;
			this.action = action;
			this.type = type;
		}
		
		public void resetCost() {
			this.cost = origineCost;
		}

		public Carte<?> ephemere() {
			this.ephemere = true;
			return this;
		}

		public Carte<?> ethereal() {
			this.ethereal = true;
			return this;
		}

		@Override
		public String toString() {
			return name;
		}
	}
	
	
	static class InteractConsole {
		
		private World world;
		
		List<Node> visited = new ArrayList<>();

		private Scanner scanner;
		
		public InteractConsole(World world) {
			this.world = world;
			scanner = new Scanner(System.in);
		}
		
		String dpath = "";
		String mpath = "map";

		public void resume(String action) {
			System.out.println(action);
		}

		public int next(int max) {
			{
				world.describe.forEach(a -> {
					if (a instanceof ActionFight) {
						ActionFight actionFight = (ActionFight) a;
						System.out.println(actionFight.origin.name() + " / " + actionFight.cible.name() + " => " +
								actionFight.effects.stream().map(e -> describe(e))
										.collect(Collectors.joining(", ","{", "}")));
					}
					else if (a instanceof ActionBuff) {
						ActionBuff actionBuff = (ActionBuff) a;
						System.out.println(actionBuff.origin.name() + " / " + actionBuff.cible.name() + " => " +
								actionBuff.effects.stream().map(e -> describe(e))
										.collect(Collectors.joining(", ","{", "}")));
					}
					else if (a instanceof ActionDeath) {
						ActionDeath actionDeath = (ActionDeath) a;
						System.out.println(actionDeath.origin.name() + " Died");
					}
				});
			}

			{
				System.out.println(world.hero.pv + "/" + world.hero.maxPv + " Gold:" + world.hero.gold +  " armure:" + world.hero.armure + " force:" + world.hero.force + " faiblesse:" + world.hero.faiblesse + " vulnerabilité:" + world.hero.vulnerability);

				List<Monster> monstre = world.monsters;
				for (int j = 0; j < monstre.size(); j++) {
					System.out.println(monstre.get(j).name() + " / " + monstre.get(j).pv +"pv / "+ " force:" + monstre.get(j).force+ " armure:" + monstre.get(j).armure + " vulnerabilité:" + monstre.get(j).vulnerability + " faiblesse:" + monstre.get(j).faiblesse + " => " + world.intentions.get(monstre.get(j)).display());

					monstre.get(j).effects.stream().filter(e -> e.display()).forEach(e -> System.out.println("   " + e.description()));
				}
			}
			
			if (world.inMap()) {
				List<Node> nexts = world.map.links.get(world.position);
				visited.add(world.position);
				displayMap(world.map.all, world.map.links, nexts, visited, mpath);
				System.out.println("Map => "+mpath);
				for (int i = 0; i< nexts.size(); i++) {
					System.out.println(i+") " + nexts.get(i).title);
				}
			}
			if (world.inChooseReward()) {
				List<ItemShop> shopItems = world.shopItems;
				System.out.println("Récompense :");
				System.out.println(" Choose or leave:");
				for (int j = 0; j < shopItems.size(); j++) {
					System.out.println((j) + ") "+ (shopItems.get(j).description().type != CarteType.GAME ? "["+ shopItems.get(j).description().cost +"] ":"") + shopItems.get(j).description().name);
				}
			}
			if (world.inShop()) {
				List<ItemShop> shopItems = world.shopItems;
				System.out.println("Magasin :");
				System.out.println(" Choose or leave:");
				for (int j = 0; j < shopItems.size(); j++) {
					System.out.println((j) + ") ["+ shopItems.get(j).price(world) +"] " + shopItems.get(j).description().name);
				}
			}
			if (world.inFight()) {
				List<Carte<?>> hand = world.hero.hand;
				System.out.println(world.hero.energy + "/" + world.hero.startEnergy);
				for (int j = 0; j < hand.size(); j++) {
					System.out.println((j) + ") "+ (hand.get(j).type != CarteType.GAME ? "["+ hand.get(j).cost +"] {"+hand.get(j).type+"} ":"") + hand.get(j).name);
				}
			}
			if (world.state == WorldState.ContextMonster) {
				List<Monster> monstre = world.monsters;
				for (int j = 0; j < monstre.size(); j++) {
					System.out.println((j) + ") " + monstre.get(j).name());
				}
				
			}
			
			System.out.println(">>");
			int choice = scanner.nextInt();
			if (choice == 77) world.monsters.forEach(m -> m.pv = 0); // XXX
			while (choice > max) {
				choice = scanner.nextInt();
			}
			dpath += choice + " ";
			if (world.inMap()) {mpath += choice;}
			
			System.out.println(" " + dpath);
			return choice;
		}

		private String describe(ActionEffect e) {
			if (e instanceof CustomActionEffect) {
				return ((CustomActionEffect) e).desc + " " + e.amount;
			}
			else if (e instanceof CarteActionEffect) {
				return ((CarteActionEffect) e).desc.name;
			}
			else if (e instanceof SpawnMonsterActionEffect) {
				return ((SpawnMonsterActionEffect) e).desc.name();
			}
			return e.type + " " + e.amount;
		}

		public void start() {
			displayMap(world.map.all, world.map.links);
		}
	}
	
	static class MapWorld {
		List<List<Node>> all;
		Map<Node, List<Node>> links;
		public Node start;

		public Node start() {
			return start;
		}	
	}
	
	enum CarteClass {
		START,
		COMMON,
		RARE,
		LEGEND,
		MALEDICTION, 
		GAME,
		;
	}
	
	public static class CarteDeck {
		private final int id;
		private final CarteType type;
		private final CarteClass classe;
		private final String description;
		private final Playable<?> action;
		private final int cost;
		private final boolean ephemere;
		private final boolean ethereal;
		
		public CarteDeck(int id, int cost, CarteType type, CarteClass classe, String description, Playable<?> action, boolean ephemere, boolean ethereal) {
			super();
			this.id = id;
			this.cost = cost;
			this.type = type;
			this.classe = classe;
			this.description = description;
			this.action = action;
			this.ephemere = ephemere;
			this.ethereal = ethereal;
		}
		
		public Carte<?> carte() {
			Carte<?> carte = new Carte<>(id, type, cost, classe, description, action);
			carte = ephemere ? carte.ephemere() : carte;
			carte = ethereal ? carte.ethereal() : carte;
			return carte;
		}
	}
	
	
	enum ActionEffectType {
		ATTACK,
		INJURY,
		FORCE,
		ARMURE,
		VULNERABILITY,
		FAIBLESSE,
		CUSTOM,
		;
	}

	public static class AttackResult {
		int amount;
		int injury;
		int brokenArmure;

		public AttackResult(int amount, int injury, int brokenArmure) {
			this.amount = amount;
			this.injury = injury;
			this.brokenArmure = brokenArmure;
		}
	}

	static class CustomActionEffect extends  ActionEffect {
		private final String desc;

		public CustomActionEffect(ActionEffectType type, int amount, String desc) {
			super(type, amount);
			this.desc = desc;
		}
	}

	static class CarteActionEffect extends  ActionEffect {
		private final Carte desc;

		public CarteActionEffect(Carte desc) {
			super(ActionEffectType.CUSTOM, 1);
			this.desc = desc;
		}
	}

	static class SpawnMonsterActionEffect extends  ActionEffect {
		private final Personnage desc;

		public SpawnMonsterActionEffect(Personnage desc) {
			super(ActionEffectType.CUSTOM, 1);
			this.desc = desc;
		}
	}

	static class ActionEffect {
		ActionEffectType type;
		int amount;

		public ActionEffect(ActionEffectType type, int amount) {
			this.type = type;
			this.amount = amount;
		}
	}

	static class Action {

	}

	static class ActionFight extends Action {
		Personnage origin;
		Personnage cible;

		List<ActionEffect> effects;

		public ActionFight(Personnage origin, Personnage cible, List<ActionEffect> effects) {
			this.origin = origin;
			this.cible = cible;
			this.effects = effects;
		}
	}

	static class ActionBuff extends Action {
		Personnage origin;
		Personnage cible;

		List<ActionEffect> effects;

		public ActionBuff(Personnage origin, Personnage cible, List<ActionEffect> effects) {
			this.origin = origin;
			this.cible = cible;
			this.effects = effects;
		}
	}
	
	
	static class ActionDeath extends Action {
		Personnage origin;
		
		
		public ActionDeath(Personnage origin) {
			this.origin = origin;
		}
	}
	
	
	
	public static class WorldDeck {
		
		private final List<CarteDeck> deck = new ArrayList<>();
		private final CarteDeck blob;
		private final CarteDeck blessure;
		private final Effects marque = new Effects() {
			public String description() {return "Marque";};
		};
		
		private final CarteDeck startAttack;
		private final CarteDeck startDefense;
		private final CarteDeck startForce;
		private final CarteDeck startVuln;
		
		private final CarteDeck endTurn;
		
		public WorldDeck() {
			
			this.endTurn = ethereal(4, CarteType.GAME,0, CarteClass.GAME, "Fin de tour", Playable.solo(World::endTurn));
			
			this.startAttack = carte(1, CarteType.ATTACK,1, CarteClass.START, "Basic Attack (6)", Playable.monster((w,m) -> w.attackMonster(m, 6)));
			this.startDefense = carte(1, CarteType.ARMURE,1, CarteClass.START, "Basic Defense (5)", Playable.solo((w) -> w.hero.armure(5, w.hero, w)));
			this.startForce = carte(2, CarteType.POWER,1, CarteClass.START, "Renforcement (+2 Force)", Playable.solo((w) -> w.hero.force(2, w)));
			this.startVuln = carte(2, CarteType.POWER,1, CarteClass.START, "Attaque Vicieuse (4, +2 Vulnerabilité)", Playable.monster((w,m) -> {w.attackMonster(m, 4); m.vulnerability(2, w.hero, w);}));
			
			this.blob = ephemere(17, CarteType.UNPLAYABLE,0, CarteClass.MALEDICTION, "Blob", Playable.notPlayable(__ -> {}));
			this.blessure = ephemere(11, CarteType.UNPLAYABLE,0, CarteClass.MALEDICTION, "Blessure (1 PV)", Playable.notPlayable(w -> w.attackHero(w.hero, 1)));
			
			carte(3, CarteType.POWER,0, CarteClass.COMMON, "Double Attack (Next attack x2)", Playable.attackStack(CarteType.ATTACK.is(), i -> i*2));
			carte(4, CarteType.POWER,0, CarteClass.COMMON, "Furie (+2 Energy, -6PV)", Playable.solo(w -> {w.hero.pv -= 6; w.hero.energy += 3;}));
			carte(5, CarteType.POWER,1, CarteClass.COMMON, "Draw (draw two cards)", Playable.solo(w -> {w.hero.drawCard();w.hero.drawCard();}));
			carte(6, CarteType.ATTACK,2, CarteClass.COMMON, "Medium Attack (15)", Playable.monster((w,m) -> w.attackMonster(m, 15)));
			carte(7, CarteType.POWER,2, CarteClass.COMMON, "Faiblesse (faiblesse +3) ", Playable.monster((w,m) -> m.faiblesse(3, w.hero, w)));
			carte(8, CarteType.ATTACK,3, CarteClass.LEGEND, "Big Attack (25)", Playable.monster((w,m) -> w.attackMonster(m, 25)));
			carte(9, CarteType.ARMURE,1, CarteClass.RARE, "Defense (7 + draw one card)", Playable.solo((w) -> {
				w.hero.armure(7, w.hero, w);
				w.hero.drawCard();}));
			carte(10, CarteType.ARMURE,1, CarteClass.RARE, "Ninja (+5 armure par attaque)", Playable.solo(w -> {w.afterPlay.add((c,z)->{if (c.type == CarteType.ATTACK) {w.hero.armure+=5;};});}));
			carte(10, CarteType.ARMURE,1, CarteClass.RARE, "BlockAttack (attaque de l'armure)", Playable.monster((w, m) -> {w.attackMonster(m, w.hero.armure);}));
			carte(12, CarteType.ATTACK,1, CarteClass.COMMON, "Sacrifice (Attack (10) + une blessure)", Playable.monster((w, m) -> {w.attackMonster(m, 10); w.hero.deck.add(blessure.carte());}));
			carte(13, CarteType.POWER,1, CarteClass.COMMON, "Marques (+2 marques à tous)", Playable.solo((w) -> {w.monsters.forEach(m -> IntStream.range(0, 2).forEach(__ -> m.effects.add(marque)));}));
			carte(14, CarteType.POWER,1, CarteClass.COMMON, "Marques (x2)", Playable.monster((w, m) -> {IntStream.range(0, (int) m.effects.stream().filter(e -> e == marque).count()).forEach(__ -> m.effects.add(marque));}));
			carte(15, CarteType.POWER,2, CarteClass.COMMON, "x5 Attaques / Marque", Playable.monster((w, m) -> {m.effects.stream().filter(e -> e == marque).forEach(__ -> w.attackMonster(m, 5));}));


		}
		
		public Carte<?> endTurn() {
			return endTurn.carte();
		}

		public List<Carte<?>> startDeck() {
			List<Carte<?>> cartes = new ArrayList<>();
			cartes.add(startAttack.carte());
			cartes.add(startAttack.carte());
			cartes.add(startAttack.carte());
			
			cartes.add(startDefense.carte());
			cartes.add(startDefense.carte());
			cartes.add(startDefense.carte());

			cartes.add(startForce.carte());
			cartes.add(startVuln.carte());
			
			return cartes;
		}
		
		public List<Carte<?>> nextDeck() {
			return deck.stream().filter(this::filterRw)
					.map(CarteDeck::carte)
					.collect(Collectors.toList());
		}

		public boolean filterRw(CarteDeck cd) {
			return cd.classe == CarteClass.COMMON || cd.classe == CarteClass.RARE || cd.classe == CarteClass.LEGEND; 
		}

		private CarteDeck carte(int __, CarteType type, int originCost, CarteClass classe, String description, Playable<?> action) {
			CarteDeck cd = new CarteDeck(deck.size(), originCost, type, classe, description, action, false, false);
			deck.add(cd);
			return cd;
		}

		/**
		 *
		 * Disparait a la fin du fight
		 */
		private CarteDeck ephemere(int __, CarteType type, int originCost, CarteClass classe, String description, Playable<?> action) {
			CarteDeck cd = new CarteDeck(deck.size(), originCost, type, classe, description, action, true, false);
			deck.add(cd);
			return cd;
		}

		/**
		 * disparait a la fin du tour
		 */
		private CarteDeck ethereal(int __, CarteType type, int originCost, CarteClass classe, String description, Playable<?> action) {
			CarteDeck cd = new CarteDeck(deck.size(), originCost, type, classe, description, action, false, true);
			deck.add(cd);
			return cd;
		}
		
	}
	
	
	public static class Description {
		String permArmure() {return "Restaure Armure +%s";}
		String increasingForce() {return "Force Grandissante +%s";}
		
	}
	
	
	public static class WorldEffects {
		
		Description descriptions;
		
		public WorldEffects(Description descriptions) {
			super();
			this.descriptions = descriptions;
		}


		public Effects permArmure(int value) {
			return new Effects() {
				@Override
				public void endTurn(Monster m, World w) {
					m.armure+= value;
				}
				
				@Override
				public String description() {
					return String.format(descriptions.permArmure(), value);
				}
			};
		}
	}
	
	
	public static class WorldBestiaire {
		
		private final WorldDeck deck;
		
		public WorldBestiaire(WorldDeck deck) {
			super();
			this.deck = deck;
		}

		public List<Monster> createMonsters() {
			List<Monster> pouf = List.of(new Monster("Pouf", 60, MonsterStrategy.prepareAndThensimpleStrategy(List.of(
					MonsterBrain.buff((m, w) -> {
						m.effects.add(new Effects() {
							@Override
							public void endTurn(Monster m, World w) {
								m.force++;
							}

							@Override
							public String description() {
								return "Force continue +1";//String.format(descriptions.permArmure(), value);
							}
						});
					})
			), List.of(
					MonsterBrain.attack(10),
					MonsterBrain.mental((m, w) -> {
						w.deck(deck.blob.carte(), m);
					}),
					MonsterBrain.attack(10)
			))));

			List<Monster> bigMac = List.of(new Monster("Big", 40, MonsterStrategy.prepareAndThensimpleStrategy(List.of(
					MonsterBrain.buff((m, w) -> {
						m.effects.add(new Effects() {
							@Override
							public void endTurn(Monster m, World w) {
								m.armure(2, m, w);
							}

							@Override
							public String description() {
								return "Perm-armure 2";
							}
						});
						w.describe(new ActionBuff(m, m, List.of(new CustomActionEffect(ActionEffectType.CUSTOM, 2, "ImproveArmure"))));
					})
			), List.of(
					MonsterBrain.attack(10)
			))), new Monster("Mac", 30, MonsterStrategy.simpleStrategy(List.of(
					MonsterBrain.buff((m, w) -> w.monsters.forEach(x -> {
						x.force += 1;
						w.describe(new ActionBuff(m, x, List.of(new ActionEffect(ActionEffectType.FORCE, 1))));
					}))
			))));

			List<Monster> soldatFou = List.of(new Monster("Soldat fou", 50, MonsterStrategy.simpleStrategy(List.of(
					MonsterBrain.attack(10),
					MonsterBrain.buff((m, w) -> {
						m.vulnerability(-m.vulnerability, m, w);
						m.force(1, w);
					}),
					MonsterBrain.mental((m, w) -> {
						w.hero.vulnerability(3, m, w);
						w.hero.faiblesse(3, m, w);
					})
			))));

			List<Monster> sponge = List.of(new Monster("Sponge", 60, MonsterStrategy.prepareAndThensimpleStrategy(List.of(
					MonsterBrain.buff((m, w) -> {
						m.effects.add(new Effects() {
							@Override
							public void endTurn(Monster m, World w) {
								w.hero.faiblesse(2, m, w);
								m.vulnerability(-m.vulnerability, m, w);
							}
						});
					})
			), List.of(
					MonsterBrain.attack(10),
					MonsterBrain.mental((m, w) -> {
						w.deck(deck.blob.carte(), m);
					})
			))));

			List<List<Monster>> allMonsters = List.of(soldatFou, pouf, bigMac, sponge);

			return allMonsters.get(random(allMonsters.size()));
		}


		private String dumbName(String from, int l, int seed) {
			String r = "";
			Random random= new Random(seed);
			for (int i = 0; i < l; i++) {
				r+=from.charAt(random.nextInt(from.length()));
			}
			return r;
		}
		public List<Monster> createEliteMonsters() {
			Random nameGuepeRandom = new Random(443134);
			Supplier<String> nameGuepe = () -> dumbName("xXwWsSzZ17", 4, nameGuepeRandom.nextInt());
			IntFunction<Monster> guepeSupplier = g -> new Monster("Guepe", 10, MonsterStrategy.simpleStrategy(List.of(
					MonsterBrain.block((m, w) -> {
						w.monsters.forEach(x -> x.armure(1, m, w));
					}),
					MonsterBrain.attack(10),
					MonsterBrain.buff((m, w) -> {
						w.monsters.forEach(x -> x.force(1, m, w));
					}),
					MonsterBrain.attack(10)
					)
			, g)).effect(new Effects() {
				@Override
				public String description() {
					return "Sbire";
				}
			});
			Monster reine = new Monster("Reine", 30, MonsterStrategy.simpleStrategy(List.of(
					MonsterBrain.block((m, w) -> {
						m.armure(5, m, w);
					}),
					MonsterBrain.attack(15),
					MonsterBrain.buff((m, w) -> {
						w.monsters.forEach(x -> x.force(2, w));
					}),
					MonsterBrain.mental((m, w) -> {
						w.monsters(guepeSupplier.apply(random(4)), m);
					})
			))).effect(new HiddenEffects() {
				@Override
				public void onDeath(Monster m, World w) {
					w.monsters.clear();
				}


			});
			List<Monster> guepes = List.of(reine, guepeSupplier.apply(0), guepeSupplier.apply(1), guepeSupplier.apply(2));
			
			List<Monster> marteau = List.of(new Monster("Marteau", 60, MonsterStrategy.prepareAndThensimpleStrategy(List.of(
					MonsterBrain.buff((m, w) -> {
						m.effects.add(new Effects() {
							@Override
							public void startTurn(Monster m, World w) {
								m.faiblesse(-m.faiblesse, m, w);
							}
							@Override
							public void endTurn(Monster m, World w) {
								m.faiblesse(-m.faiblesse, m, w);
								m.force(2, w);
							}
						});
					})
			), List.of(
					MonsterBrain.attack(10)
			))));
			
			var allMonsters = List.of(guepes, marteau);
			return allMonsters.get(random(allMonsters.size()));
		}
	}
	
	
	
	
	
	public static void main0(String[] args) throws IOException {
		int seed= 16486744;
		WorldDeck deck = new WorldDeck();
		WorldBestiaire bestiaire = new WorldBestiaire(deck);

		MapGenerator generator = new MapGenerator(seed, 0, deck, bestiaire);
		for (int i = 0; i < 10; i++) {

			MapWorld map = generator.createMap();
			displayMap(map.all, map.links, "a"+i);
		}

	}
	public static void main(String[] args) throws IOException {
		WorldDeck deck = new WorldDeck();
		WorldBestiaire bestiaire = new WorldBestiaire(deck);
		
		int seed= 16486744;
		MapGenerator generator = new MapGenerator(seed, 0, deck, bestiaire);
		MapWorld map = generator.createMap();
		
		Heros heros = new Heros();
		heros.maxPv = 40;
		heros.pv = 30;
		heros.deck = shuffle(deck.startDeck());
		
		World world = new World(map, heros, deck);
		
		InteractConsole interact = new InteractConsole(world);
		interact.start();

		play(heros, world, interact);
	}

	private static void play(Heros heros, World world, InteractConsole interact) {
		while (!world.isEnded()) {
			if (world.inMap()) {
				List<Node> nexts = world.map.links.get(world.position);
				int nextNode = interact.next(nexts.size()-1);

				world.moveTo(nexts.get(nextNode));
			}
			else if (world.inShop()) {
				int nextNode = interact.next(world.shopItems.size()-1);
				world.playChooseItem(world.shopItems.get(nextNode));
			}
			else if (world.inChooseReward()) {
				int nextNode = interact.next(world.shopItems.size()-1);
				world.playChooseItem(world.shopItems.get(nextNode));
			}
			else if (world.inFight()) {
				int nextNode = interact.next(heros.hand.size()-1);
				world.playCard(world.hero.hand.get(nextNode));
			}
			else if (world.state == WorldState.ContextSolo) {
				world.play(__ -> world);
			}
			else if (world.state == WorldState.ContextMonster) {
				int monsterNode = interact.next(world.monsters.size() - 1);
				world.play(__ -> world.monsters.get(monsterNode));
			}
			else if (world.state == WorldState.ContextDeck) {
				int node = interact.next(world.hero.deck.size() - 1);
				world.play(__ -> world.hero.deck.get(node));
			}

		}
	}

	private static void displayMap(List<List<Node>> all, Map<Node, List<Node>> links) {
		String fname = "ex1";
		displayMap((List<List<Node>>) all, (Map<Node, List<Node>>) links, fname);
	}

	private static void displayMap(List<List<Node>> all, Map<Node, List<Node>> links, String fname) {
		displayMap(all, links, Collections.emptyList(), Collections.emptyList(), fname);
	}

	private static void displayMap(List<List<Node>> all, Map<Node, List<Node>> links, List<Node> highlight, List<Node> crossed, String fname) {
		List<LinkSource> linksSource = new ArrayList<>();
		
		links.entrySet()
		.stream()
		.filter(e -> e.getKey().type != NodeType.START)
		.forEach(e -> e.getValue()
				.stream()
				.filter(n -> n.type != NodeType.END)
				.forEach(n -> linksSource.add(node(e.getKey().title).link(to(node(n.title)).with(attr("weight", 100))))));
		
		linksSource.add(graph().graphAttr().with(Rank.inSubgraph(RankType.SAME)).with(node("A"), node("B")));
		linksSource.add(graph().graphAttr().with(Rank.inSubgraph(RankType.SAME)).with(node("A"), node("C")));
		all.stream().flatMap(List::stream).forEach(x -> {
			if (x.type == NodeType.SHOP) {
				linksSource.add(node(x.title).with(Color.RED));
				linksSource.add(node(x.title).with("label", "Shop"));
			}
			if (x.type == NodeType.ELITE) {
				linksSource.add(node(x.title).with(Color.BLUEVIOLET));
			}
			if (x.type == NodeType.COFFRE) {
				linksSource.add(node(x.title).with(Color.CORAL4));
				linksSource.add(node(x.title).with("label", "Trésor"));
			}
			if (x.type == NodeType.REPOS) {
				linksSource.add(node(x.title).with(Color.CHARTREUSE4));
				linksSource.add(node(x.title).with("label", reposName[x.h % reposName.length]));
			}
			if (x.type == NodeType.BOSS) {
				linksSource.add(node(x.title).with("label", "BOSS"));
			}
			if (x.type == NodeType.ENIGME) {
				linksSource.add(node(x.title).with(Color.VIOLETRED4));
				linksSource.add(node(x.title).with("label", "?"));
			}
			if (x.type == NodeType.MONSTER) {
				//linksSource.add(node(x.title).with(Image.of("./datas/goblin-head.png")));
			}
		});
		highlight.stream().forEach(x -> {
			linksSource.add(node(x.title).with("shape", "doublecircle"));
		});
		crossed.stream().filter(x -> x.type != NodeType.START).forEach(x -> {
			linksSource.add(node(x.title).with("style", "filled"));
		});

		for (int i = 0; i < all.get(0).size() - 1; i++) {
			linksSource.add(node(all.get(0).get(i).title).link(to(node(all.get(0).get(i + 1).title)).with(attr("style", "invis"))));
		}



		Graph g = graph("example1").directed()
		        .graphAttr().with(Rank.dir(RankDir.BOTTOM_TO_TOP))
		        .nodeAttr().with(Font.name("Ink Free"))
		        .linkAttr().with("class", "link-class")
		        .with(linksSource);
		
		
		try {
				Graphviz.fromGraph(g)
				.processor(new Roughifyer()
				        .bowing(2)
				        .curveStepCount(6)
				        .roughness(1)
				        .fillStyle(FillStyle.hachure().width(2).gap(5).angle(30))
				        .font("*serif", "Comic Sans MS"))
				.height(600).render(Format.PNG).toFile(new File("examples/"+fname+".png"));
				Graphviz.fromGraph(g)
				.render(Format.DOT).toFile(new File("examples/"+fname+".dot"));
		} catch (IOException e1) {
			throw new RuntimeException("Cannot save display", e1);
		}
	}
	
	
	static class ElementGenerator {
		
		private final WorldDeck deck;
		private final WorldBestiaire bestiaire;
		
		public ElementGenerator(WorldDeck deck, WorldBestiaire bestiaire) {
			super();
			this.deck = deck;
			this.bestiaire = bestiaire;
		}

		public List<ItemShop> createReward(Node n) {
			List<Carte<?>> carteList = shuffle(deck.nextDeck());
			List<ItemShop> shop = new ArrayList<>();
			shop.add(leave("Aucune"));
			
			for (int i = 0; i < 3; i++) {
				Carte<?> carte = carteList.get(i);
				shop.add(ItemShop.carte(0, carte));
			}
			
			return shop;
		}
		
		public List<ItemShop> createShop(Node n) {
			List<Carte<?>> carteList = shuffle(deck.nextDeck());
			List<ItemShop> shop = new ArrayList<>();
			String label = "Quitter la boutique";
			shop.add(leave(label));
			
			for (int i = 0; i < 6; i++) {
				Carte<?> carte = carteList.get(i);
				int price = (int) (carte.classe.ordinal() * 25 * (random(100) + 50) / 100.);
				shop.add(ItemShop.carte(price, carte));
			}
			shop.add(new ItemShop() {

				@Override
				public int price(World world) {
					return 25 * (world.cardsRemoved + 1);
				}
				
				@Override
				public Carte<?> description() {
					return new Carte<Carte<?>>(-1, CarteType.POWER, 0, CarteClass.GAME, "Enlève une carte", null);
				}
				
				@Override
				public Playable<?> buy(World world) {
					return new Playable<Carte<?>>() {

						@Override
						public ContextType type() {
							return ContextType.DECK;
						}

						@Override
						public boolean play(World world, Context<Carte<?>> context) {
							Carte<?> toRemove = context.from(world);
							world.hero.deck.remove(toRemove);
							return true;
						}
						
					};
				}
			});
			
			return shop;
		}

		public ItemShop leave(String label) {
			return new ItemShop() {

				@Override
				public int price(World __) {
					return 0;
				}
				
				@Override
				public Carte<?> description() {
					return new Carte<Carte<?>>(-1, CarteType.GAME, 0, CarteClass.GAME, label, null);
				}
				
				@Override
				public Playable<?> buy(World world) {
					return new Playable<World>() {

						@Override
						public boolean play(World world, Context<World> context) {
							world.state = WorldState.Map;
							return true;
						}

						@Override
						public ContextType type() {
							return ContextType.SOLO;
						}};
				
				}
			};
		}

		public List<Monster> createMonster(Node node) {
			List<Monster> monsters = bestiaire.createMonsters();
			levelUpMonsters(node, monsters);
			return monsters;
		}

		public List<Monster> createElite(Node node) {
			List<Monster> monsters = bestiaire.createEliteMonsters();
			levelUpMonsters(node, monsters);
			return monsters;
		}
		
		private void levelUpMonsters(Node node, List<Monster> monsters) {
			for (Monster m : monsters) {
				m.originPv((int) (m.originPv * (node.level + 1)/2. + node.h));
			}
		}
	}
	
	
	static class MapGenerator {
		private final Random randomMap;
		private final ElementGenerator elements;
		private final int level;
		
		public MapGenerator(int seed, int level, WorldDeck deck, WorldBestiaire bestiaire) {
			this.level = level;
			this.randomMap = new Random(seed);
			this.elements = new ElementGenerator(deck, bestiaire);
		}
		
		int randomMap(int n) {
			return randomMap.nextInt(n);
		}
		
		public MapWorld createMap() {
			List<List<Node>> all = generateNodes();

			Map<Node, List<Node>> links = generatePath(all);
			differentiateNode(all, links);
			animateNode(all);
			
			Node start = new Node("Start", level, 0).type(NodeType.START);
			Node end = new Node("End", level, 0).type(NodeType.END);
			links.put(start, new ArrayList<>(all.get(0)));
			links.put(all.get(all.size() - 1).get(0), List.of(end));
			
			MapWorld map = new MapWorld();
			map.all = all;
			map.links = links;
			map.start = start;
			return map;
		}

		private void animateNode(List<List<Node>> all) {
			all.stream().flatMap(List::stream)
				.forEach(n -> {
					if (n.type == NodeType.MONSTER) {
						List<Monster> monsters = elements.createMonster(n);
						n.setMonsters(() -> monsters);
						List<ItemShop> items = elements.createReward(n);
						int gold = random(25 * (level + 1) + n.h) + 25;
						Reward reward = new Reward(gold, () -> items);
						n.setReward(reward);
					}
					else if (n.type == NodeType.ELITE) {
						List<Monster> monsters = elements.createElite(n);
						n.setMonsters(() -> monsters);
						List<ItemShop> items = elements.createReward(n);
						int gold = 2 * (random(25 * (level + 1) + n.h) + 25);
						Reward reward = new Reward(gold, () -> items);
						n.setReward(reward);
					}
					else if (n.type == NodeType.SHOP) {
						List<ItemShop> items = elements.createShop(n);
						n.setReward(new Reward(0, () -> items));
					}
					else if (n.type == NodeType.COFFRE) {
						List<ItemShop> items = elements.createShop(n);
						n.setReward(new Reward(0, () -> items));
					}
					else {
						n.setMonsters(() -> Collections.emptyList());
						n.setReward(new Reward(0, () -> Collections.emptyList()));
					}
					
				});
		}

		private void differentiateNode(List<List<Node>> all, Map<Node, List<Node>> links) {
			List<Node> inside = all.stream().skip(1).flatMap(List::stream).filter(x -> x.type == NodeType.MONSTER).collect(Collectors.toList());

			List<Node> starting = all.get(0);

			boolean allPathHasShop = false;
			while (!allPathHasShop) {
				{
					Function<Node, Predicate<Node>> predicate  =m -> x -> links.get(x).contains(m) && x.type == NodeType.SHOP;

					Node n = inside.get(randomMap(inside.size()));
					while (links.get(n).stream().anyMatch(x -> x.type == NodeType.COFFRE)
							|| links.keySet().stream().anyMatch(predicate.apply(n))) {
						n = inside.get(randomMap(inside.size()));
					}
					n.type = NodeType.SHOP;
				}
				allPathHasShop = true;
				for (Node startingNode : starting) {
					boolean pathHasShop = false;
					boolean pathHas2Shop = false;
					Set<Node> current = new HashSet<>(Collections.singleton(startingNode));
					while (!current.isEmpty()) {
						Set<Node> next = new HashSet<>();
						for (Node n : current) {
							List<Node> linked = links.getOrDefault(n, Collections.emptyList());
							next.addAll(linked);
						}
						current = next;
						for (Node n : current) {
							if (n.type == NodeType.SHOP) {
								if (pathHasShop ) {
									pathHas2Shop = true;
								}
								pathHasShop = true;
							}
						}
					}

					if (!pathHas2Shop) {
						allPathHasShop = false;
					}
				}
			}

			for (int i = 0; i < 5; i++) {
				Node n = inside.get(randomMap(inside.size()));
				while (n.type != NodeType.MONSTER) {
					n = inside.get(randomMap(inside.size()));

				}
				n.type = NodeType.ELITE;
			}

			for (int i = 0; i < 4; i++) {
				Node n = inside.get(randomMap(inside.size()));
				Function<Node, Predicate<Node>> predicate  =m -> x -> links.get(x).contains(m) && x.type == NodeType.REPOS;
				while (n.type != NodeType.MONSTER
						|| links.get(n).stream().anyMatch(x -> x.type == NodeType.REPOS)
						|| links.keySet().stream().anyMatch(predicate.apply(n))) {
					n = inside.get(randomMap(inside.size()));
				}
				n.type = NodeType.REPOS;
			}
			for (int i = 0; i < 8; i++) {

				Node n = inside.get(randomMap(inside.size()));
				while (n.type != NodeType.MONSTER) {
					n = inside.get(randomMap(inside.size()));
				}
				n.type = NodeType.ENIGME;
			}
			for (int i = 0; i < 1; i++) {
				Node n = inside.get(randomMap(inside.size()));
				Function<Node, Predicate<Node>> predicate  =m -> x -> links.get(x).contains(m) && x.type == NodeType.COFFRE;
				while (n.type != NodeType.MONSTER
						|| links.get(n).stream().anyMatch(x -> x.type == NodeType.COFFRE)
						|| links.keySet().stream().anyMatch(predicate.apply(n))) {
					n = inside.get(randomMap(inside.size()));
				}
				n.type = NodeType.COFFRE;
			}
			all.get(all.size()-2).forEach(x -> x.type = NodeType.REPOS);
		}

		private  List<List<Node>> generateNodes() {
			List<List<Node>> all = new ArrayList<>();
			int steps = 15;
			all.add(List.of(new Node("A", level, 0), new Node("B", level, 1), new Node("C", level, 2)));
			int n = 0;
			for (int i = 0; i < steps; i++) {
				int nbNode = randomMap(2) + 3;
				List<Node> nodes = new ArrayList<>();
				for (int j = 0; j < nbNode; j++) {
					nodes.add(new Node((++n)+"", level, j));
				}
				all.add(nodes);
			}
			all.add(List.of(new Node("D", level, 0), new Node("E", level, 1), new Node("F", level, 2)));
			Node finalBoss = new Node("X", level, 0);
			finalBoss.type = NodeType.ELITE;
			all.add(List.of(finalBoss));
			all.get(steps / 2).forEach(x -> x.type = NodeType.COFFRE);
			return all;
		}

		private int sq(int x) { return  x*x;}
		private  Map<Node, List<Node>> generatePath(List<List<Node>> all) {
			Map<Node, Set<Node>> links = new HashMap<>();
			Set<Node> allNode = all.stream().flatMap(List::stream).collect(Collectors.toSet());
			while (!allNode.isEmpty()) {
				int fromLayer = randomMap(all.size()-1);
				int from = randomMap(all.get(fromLayer).size());
				int to = randomMap(all.get(fromLayer + 1).size());

				Node nodeFrom = all.get(fromLayer).get(from);
				Node nodeTo = all.get(fromLayer + 1).get(to);

				if (!allNode.contains(nodeFrom) && !allNode.contains(nodeTo)) {
					continue;
				}

				if (!allNode.contains(nodeFrom) || !allNode.contains(nodeTo)) {
					if (randomMap(100) > 1)
						continue;
				}

				if (randomMap(100)  < sq(links.getOrDefault(nodeFrom, Collections.emptySet()).size()) *  6) {
					continue;
				}

				if (randomMap(100)  < sq((int) links.values().stream().filter(x -> x.contains(nodeTo)).count()) *  6) {
					continue;
				}

				if (randomMap(100)  > 1 && links.values().stream().filter(x -> x.contains(nodeTo)).count() > 2) {
					continue;
				}

				double r_min = (from - 0.5) / all.get(fromLayer).size();
				double r_max = (from + 0.5) / all.get(fromLayer).size();

				double x_min = ((double) to - 0.5) / all.get(fromLayer + 1).size();
				double x_max = ((double) to + 0.5) / all.get(fromLayer + 1).size();

				boolean possible = (x_min >= r_min && x_min <= r_max) || (x_max >= r_min && x_max <= r_max) || (randomMap(100) >95);
				for (int j = 0; j < from; j++) {
					Set<Node> p = links.getOrDefault(all.get(fromLayer).get(j), Collections.emptySet());
					for (Node n : p) {
						if (n.h > to) {
							possible = false;
						}
					}
				}

				for (int j = from + 1; j < all.get(fromLayer).size(); j++) {
					Set<Node> p = links.getOrDefault(all.get(fromLayer).get(j), Collections.emptySet());
					for (Node n : p) {
						if (n.h < to) {
							possible = false;
						}
					}
				}

				if (possible) {
					links.computeIfAbsent(nodeFrom, __ -> new HashSet<>()).add(nodeTo);
				}

				for (Node node : new HashSet<>(allNode)) {
					if (links.values().stream().flatMap(Set::stream).anyMatch(n -> n == node) 
							|| node.title.equals("A")|| node.title.equals("B")|| node.title.equals("C") || node.title.equals("D") || node.title.equals("E")) {
						if (links.containsKey(node) || node.title.equals("X")) {
							allNode.remove(node);
						}
					}
				}
			}
			Map<Node, List<Node>> res = new HashMap<>();
			for (Entry<Node, Set<Node>> e : links.entrySet()) {
				ArrayList<Node> nodes = new ArrayList<>(e.getValue());
				Collections.sort(nodes, Comparator.comparing(x -> x.h));
				res.put(e.getKey(), nodes);
			}
			return res;
		}

	}
	
}
