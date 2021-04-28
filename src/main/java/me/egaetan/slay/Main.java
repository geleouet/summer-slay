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
		NodeType type;
		
		
		public Node(String title, int h) {
			this.title = title;
			this.h = h;
			this.type = NodeType.MONSTER;
		}


		public Node type(NodeType type) {
			this.type = type;
			return this;
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
		public MonsterBrain get(World world, Monster monster);
		
		public static MonsterStrategy simpleStrategy(List<MonsterBrain> brains) {
			return simpleStrategy(brains, 0);
		}

		public static MonsterStrategy prepareAndThensimpleStrategy(List<MonsterBrain> brainsPrepare, List<MonsterBrain> brains) {
			return andThenSimpleStrategy(brainsPrepare, brains, 0);
		}

		public static MonsterStrategy andThenSimpleStrategy(List<MonsterBrain> brainsPrepare, List<MonsterBrain> brains, int start) {
			return  new MonsterStrategy() {

				int state = 0;
				boolean inPrepare=true;

				@Override
				public MonsterBrain get(World world, Monster monster) {
					if (state == brainsPrepare.size() && inPrepare) {
						inPrepare = false;
						state = start;
					}
					if (inPrepare) {
						MonsterBrain monsterBrain = brainsPrepare.get(state);
						state ++;
						return monsterBrain;
					}

					MonsterBrain monsterBrain = brains.get(state);
					state = (state + 1) % brains.size();
					return monsterBrain;
				}
			};
		}public static MonsterStrategy simpleStrategy(List<MonsterBrain> brains, int start) {
			return  new MonsterStrategy() {

				int state = start;

				@Override
				public MonsterBrain get(World world, Monster monster) {
					MonsterBrain monsterBrain = brains.get(state);
					state = (state + 1) % brains.size();
					return monsterBrain;
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

	interface Effects {
		default void onDeath(Monster m, World w) {}

		default void endTurn(Monster m, World w) {};
	}

	public static class Monster {
		String name;
		int originPv;
		
		int pv;
		int force;
		int armure;
		int vulnerability;
		int faiblesse;

		List<Effects> effects = new ArrayList<>();

		private MonsterStrategy brain;

		public Monster(String name, int pv, MonsterStrategy brain) {
			this.name = name;
			this.originPv = pv;
			this.pv = pv;
			this.brain = brain;
		}

		public void attackHero(World world, int a) {
			world.attackHero((int) ((a + force) * (faiblesse > 0 ? 0.75 : 1.)));
		}

		public void attack(int degat) {
			this.pv = (int) Math.max(0, this.pv - Math.max(0, degat - armure) * (vulnerability > 0 ? 1.5: 1.));
		}

		public String name() {
			return name;
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
	
	public static class Heros {
		int pv;
		int force;
		int armure;
		int vulnerability;
		int faiblesse;
		
		int energy;
		int maxPv;
		
		int startEnergy = 3;
		int gold = 50;

		int handSize = 5;
		List<Carte<?>> deck = new ArrayList<>();
		List<Carte<?>> hand = new ArrayList<>();
		List<Carte<?>> played = new ArrayList<>();
		List<Carte<?>> retired = new ArrayList<>();


		public void addDefense(int d) {
			armure += d;
		}

		public void addForce(int f) {
			force += f;
		}

		public void attack(int a) {
			pv -= Math.max(0, a - armure) * (vulnerability > 0 ? 1.5 : 1.);
		}

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
			tmp.forEach(c -> {if (!c.ephemere) deck.add(c);});
		}
	}
	
	enum WorldState {
		Map,
		Fight,
		ChoseReward,
		;
	}
	
	public static class World {
		Heros hero;
		List<Monster> monsters;
		Map<Monster, MonsterBrain> intentions = new HashMap<>();
		
		private Node position = null;
		private MapWorld map;
		private WorldState state;

		private Carte<?> currentCard;
		
		List<BiConsumer<Carte<?>, World>> beforePlay = new ArrayList<>();
		List<BiConsumer<Carte<?>, World>> afterPlay = new ArrayList<>();
		
		List<IntUnaryOperator> attackStack = new ArrayList<>();
 		
		
		public World(MapWorld map, Heros heros) {
			this.map = map;
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

		public void moveTo(Node node) {
			position = node;
			if (position.type == NodeType.MONSTER) {
				state = WorldState.Fight;
				monsters = new ArrayList<>(createMonsters());
				startFight();
				startTurn();
			}
			if (position.type == NodeType.ELITE) {
				state = WorldState.Fight;
				monsters = new ArrayList<>(createEliteMonsters());
				startFight();
				startTurn();
			}
		}

		private List<Carte<?>> createChooseReward() {
			List<Carte<?>> carteList = shuffle(nextDeck());
			return List.of(carteList.get(0), carteList.get(1), carteList.get(2));
		}

		private List<Monster> createMonsters() {
			List<Monster> pouf = List.of(new Monster("Pouf", 60, MonsterStrategy.prepareAndThensimpleStrategy(List.of(
					MonsterBrain.buff((m, w) -> {
						m.effects.add(new Effects() {
							@Override
							public void endTurn(Monster m, World w) {
								m.force++;
							}
						});
					})
			), List.of(
					MonsterBrain.attack(10),
					MonsterBrain.mental((m, w) -> {
						w.hero.deck.add(blobCarteSupplier.get());
					}),
					MonsterBrain.attack(10)
			))));

			List<Monster> bigMac = List.of(new Monster("Big", 40, MonsterStrategy.prepareAndThensimpleStrategy(List.of(
					MonsterBrain.buff((m, w) -> {
						m.effects.add(new Effects() {
							@Override
							public void endTurn(Monster m, World w) {
								m.armure+=2;
							}
						});
					})
			), List.of(
					MonsterBrain.attack(10)
			))), new Monster("Mac", 30, MonsterStrategy.simpleStrategy(List.of(
					MonsterBrain.buff((m, w) -> w.monsters.forEach(x -> x.force++))
			))));

			List<Monster> soldatFou = List.of(new Monster("Soldat fou", 50, MonsterStrategy.simpleStrategy(List.of(
					MonsterBrain.attack(10),
					MonsterBrain.buff((m, w) -> {
						m.vulnerability = 0;
						m.force += 1;
					}),
					MonsterBrain.mental((m, w) -> {
						w.hero.vulnerability += 3;
						w.hero.faiblesse += 3;
					})
			))));

			List<Monster> sponge = List.of(new Monster("Sponge", 60, MonsterStrategy.prepareAndThensimpleStrategy(List.of(
					MonsterBrain.buff((m, w) -> {
						m.effects.add(new Effects() {
							@Override
							public void endTurn(Monster m, World w) {
								w.hero.faiblesse++;
								m.vulnerability = 0;
							}
						});
					})
			), List.of(
					MonsterBrain.attack(10),
					MonsterBrain.mental((m, w) -> {
						w.hero.deck.add(blobCarteSupplier.get());
					})
			))));

			List<List<Monster>> allMonsters = List.of(soldatFou, pouf, bigMac, sponge);

			return allMonsters.get(random(allMonsters.size()));
		}

		private List<Monster> createEliteMonsters() {
			IntFunction<Monster> guepeSupplier = g -> new Monster("Guepe", 10, MonsterStrategy.simpleStrategy(List.of(
					MonsterBrain.block((m, w) -> {
						w.monsters.forEach(x -> x.armure += 1);
					}),
					MonsterBrain.attack(10),
					MonsterBrain.buff((m, w) -> {
						w.monsters.forEach(x -> x.force += 1);
					}),
					MonsterBrain.attack(10)
					)
			, g));
			Monster reine = new Monster("Reine", 30, MonsterStrategy.simpleStrategy(List.of(
					MonsterBrain.block((m, w) -> {
						m.armure += 5;
					}),
					MonsterBrain.attack(15),
					MonsterBrain.buff((m, w) -> {
						w.monsters.forEach(x -> x.force += 2);
					}),
					MonsterBrain.mental((m, w) -> {
						w.monsters.add(guepeSupplier.apply(random(4)));
					})
			)));
			return List.of(reine, guepeSupplier.apply(0), guepeSupplier.apply(1), guepeSupplier.apply(2));
		}

		private void attackHero(int a) {
			hero.attack(a);
		}

		public void attack(Monster m, int f) {
			int attack = hero.attack(m, f);
			for (IntUnaryOperator i : attackStack) {
				attack = i.applyAsInt(attack);
			}
			m.attack(attack);
		}

		public void startFight() {
			shuffle(hero.deck);
			for (Carte<?> c : hero.deck) c.resetCost();
			intentions.clear();
			hero.vulnerability = 0;
			hero.faiblesse = 0;
			hero.armure = 0;
			hero.force = 0;
		}
		
		public void startTurn() {
			hero.energy = hero.startEnergy ;
			hero.vulnerability = Math.max(0, hero.vulnerability - 1);
			hero.faiblesse = Math.max(0, hero.faiblesse - 1);
			hero.armure = 0;

			for (int i = 0; i < hero.handSize; i++) {
				hero.drawCard();
			}
			for (Monster m : monsters) {
				if (m.pv > 0) {
					intentions.put(m, m.brain.get(this, m));
				}
			}
		}
		
		public void endTurn() {
			hero.played.addAll(hero.hand);
			hero.hand.clear();

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
			checkEndFight();
			if (inFight()) {
				startTurn();
			}
		}

		private void checkEndFight() {
			List<Monster> killed = new ArrayList<>();
			for (Monster m : monsters) {
				if (m.pv <= 0) {
					m.effects.forEach(e -> e.onDeath(m, this));
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

			startChooseReward();
		}

		private void startChooseReward() {
			state = WorldState.ChoseReward;
			hero.hand = new ArrayList<>(createChooseReward());
			hero.winGold(random(25) + 25);
		}

		public void chooseReward(int nextNode) {
			if (nextNode != 0) {
				Carte<?> chosen = hero.hand.get(nextNode - 1);
				hero.hand.clear();
				hero.deck.add(chosen);
				state = WorldState.Map;
			}
		}

		public void play(Context<Monster> context) {
			carteMonster(currentCard).action.play(this, context);
			afterPlay();
		}
		public void play(Carte<?> carte) {
			hero.energy -= carte.cost;
			this.currentCard = carte;
			
			beforePlay(carte);

			if (carte.action.type() == ContextType.SOLO) {
				carteWorld(carte).action.play(this, __ -> World.this);
				afterPlay();
			}
			
			if (carte.action.type() == ContextType.MONSTER) {
				this.currentCard = carte;
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

		@SuppressWarnings("unchecked")
		private Carte<World> carteWorld(Carte<?> carte) {
			return (Carte<World>) carte;
		}

		@SuppressWarnings("unchecked")
		private Carte<Monster> carteMonster(Carte<?> carte) {
			return (Carte<Monster>) carte;
		}

	}
	
	public static interface Context<T> {
		T from(World w);
	}
	
	enum ContextType {
		SOLO(World.class),
		MONSTER(Monster.class),
		
		;

		private Class<?> class_;

		ContextType(Class<?> class_) {
			this.class_ = class_;
		}
	}
	
	public static interface Playable<T> {
		static Playable<World>  notPlayable(Consumer<World> onDiscard) {
			return new Playable<World>() {
				@Override
				public void discard(World world) {
					onDiscard.accept(world);
				}

				@Override
				public void play(World world, Context<World> context) {}

				@Override
				public ContextType type() {
					return ContextType.SOLO;
				}
			};
		}

		default void discard(World world) {};
		void play(World world, Context<T> context);
		ContextType type();
		
		static Playable<World> solo(Consumer<World> action) {
			return new Playable<World>() {
				@Override
				public void play(World world, Context<World> context) {
					action.accept(world);
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
				public void play(World world, Context<Monster> context) {
					action.accept(world, context.from(world));
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
		OOPS,
		;
		
		Predicate<Carte<?>> is() {
			return c -> c.type == this; 
		}
	}
	
	public static class Carte<T> {
		int id;
		int cost;
		int classe;
		int origineCost;

		boolean ephemere = false;

		String name;
		Playable<T> action;
		CarteType type;
		
		public Carte(int id, CarteType type, int originEnergy, int classe, String name, Playable<T> action) {
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
		
		String dpath = "m";

		public void resume(String action) {
			System.out.println(action);
		}

		public int next(int max) {
			if (world.inMap()) {
				List<Node> nexts = world.map.links.get(world.position);
				visited.add(world.position);
				displayMap(world.map.all, world.map.links, nexts, visited, dpath);
				System.out.println("Map => "+dpath);
				for (int i = 0; i< nexts.size(); i++) {
					System.out.println(i+") " + nexts.get(i).title);
				}
				System.out.println(">>");
				int choice = scanner.nextInt();
				while (choice > max) {
					choice = scanner.nextInt();
				}
				dpath += choice;
				return choice;
			}
			if (world.inChooseReward()) {
				List<Carte<?>> hand = world.hero.hand;
				System.out.println("Choose a card:");
				System.out.println("0) Aucune");
				for (int j = 0; j < hand.size(); j++) {
					System.out.println((j+1) + ") ["+ hand.get(j).cost +"] " + hand.get(j).name);
				}
				System.out.println(">>");
				int choice = scanner.nextInt();
				while (choice > max) {
					choice = scanner.nextInt();
				}
				return choice;
			}
			if (world.inFight()) {
				{
					System.out.println(world.hero.pv + "/" + world.hero.maxPv + " force:" + world.hero.force + " faiblesse:" + world.hero.faiblesse + " vulnerabilité:" + world.hero.vulnerability);

					List<Monster> monstre = world.monsters;
					for (int j = 0; j < monstre.size(); j++) {
						System.out.println(monstre.get(j).name() + " / " + monstre.get(j).pv +"pv / "+ " force:" + monstre.get(j).force + " vulnerabilité:" + monstre.get(j).vulnerability + " faiblesse:" + monstre.get(j).faiblesse + " => " + world.intentions.get(monstre.get(j)).display());
					}
				}
				
				if (world.currentCard == null) {
					List<Carte<?>> hand = world.hero.hand;
					System.out.println(world.hero.energy + "/" + world.hero.startEnergy);
					System.out.println("0) fin du tour");
					for (int j = 0; j < hand.size(); j++) {
						System.out.println((j+1) + ") ["+ hand.get(j).cost +"] " + hand.get(j).name);
					}
					System.out.println(">>");
					int choice = scanner.nextInt();
					if (choice == 77) world.monsters.forEach(m -> m.pv = 0);
					while (choice > max) {
						choice = scanner.nextInt();
					}
					return choice;
				}
				else {
					List<Monster> monstre = world.monsters;
					for (int j = 0; j < monstre.size(); j++) {
						System.out.println((j) + ") " + monstre.get(j).name());
					}
					System.out.println(">>");
					int choice = scanner.nextInt();
					while (choice > max) {
						choice = scanner.nextInt();
					}
					return choice;
				}
				
			}
			
			
			return 0;
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
	
	
	static List<Carte<?>> startDeck() {
		List<Carte<?>> cartes = new ArrayList<>();
		
		cartes.add(new Carte<>(0, CarteType.ATTACK, 1, 0, "Basic Attack (6)", Playable.monster((w,m) -> w.attack(m, 6))));
		cartes.add(new Carte<>(0, CarteType.ATTACK,1, 0, "Basic Attack (6)", Playable.monster((w,m) -> w.attack(m, 6))));
		cartes.add(new Carte<>(0, CarteType.ATTACK,1, 0, "Basic Attack (6)", Playable.monster((w,m) -> w.attack(m, 6))));
		
		cartes.add(new Carte<>(1, CarteType.ARMURE,1, 0, "Basic Defense (5)", Playable.solo((w) -> w.hero.addDefense(5))));
		cartes.add(new Carte<>(1, CarteType.ARMURE,1, 0, "Basic Defense (5)", Playable.solo((w) -> w.hero.addDefense(5))));
		cartes.add(new Carte<>(1, CarteType.ARMURE,1, 0, "Basic Defense (5)", Playable.solo((w) -> w.hero.addDefense(5))));
		
		cartes.add(new Carte<>(2, CarteType.POWER,1, 0, "Renforcement (+2 Force)", Playable.solo((w) -> w.hero.addForce(2))));

		return cartes;
	}

	static Supplier<Carte<?>> blobCarteSupplier = () ->new Carte<>(17, CarteType.OOPS,0, 99, "Blob", Playable.notPlayable(__ -> {})).ephemere();

	static List<Carte<?>> nextDeck() {
		List<Carte<?>> cartes = new ArrayList<>();
		
		cartes.add(new Carte<>(3, CarteType.POWER,0, 1, "Double Attack (Next attack x2)", Playable.attackStack(CarteType.ATTACK.is(), i -> i*2)));
		cartes.add(new Carte<>(4, CarteType.POWER,0, 1, "Furie (+2 Energy, -6PV)", Playable.solo(w -> {w.hero.pv -= 6; w.hero.energy += 3;})));
		cartes.add(new Carte<>(5, CarteType.POWER,1, 1, "Draw (draw two cards)", Playable.solo(w -> {w.hero.drawCard();w.hero.drawCard();})));
		cartes.add(new Carte<>(6, CarteType.ATTACK,2, 1, "Medium Attack (15)", Playable.monster((w,m) -> w.attack(m, 15))));
		cartes.add(new Carte<>(7, CarteType.POWER,2, 2, "Faibless", Playable.monster((w,m) -> m.vulnerability +=3)));
		cartes.add(new Carte<>(8, CarteType.ATTACK,3, 3, "Big Attack (25)", Playable.monster((w,m) -> w.attack(m, 25))));
		cartes.add(new Carte<>(9, CarteType.ARMURE,1, 3, "Defense (7 + draw one card)", Playable.solo((w) -> {w.hero.addDefense(5); w.hero.drawCard();})));
		cartes.add(new Carte<>(10, CarteType.ARMURE,1, 3, "BlockAttack (attaque de l'armure)", Playable.monster((w, m) -> {w.attack(m, w.hero.armure);})));

		Supplier<Carte<?>> blessures = () ->new Carte<>(11, CarteType.OOPS,0, 99, "Blessure (1 PV)", Playable.notPlayable(w -> w.hero.pv -= 1)).ephemere();
		cartes.add(new Carte<>(12, CarteType.ATTACK,1, 3, "Sacrifice (Attack (10) + une blessure)", Playable.monster((w, m) -> {w.attack(m, 10); w.hero.deck.add(blessures.get());})));
		Effects MARQUE = new Effects() {};
		cartes.add(new Carte<>(13, CarteType.POWER,1, 4, "Marques (+5 marques à tous)", Playable.solo((w) -> {w.monsters.forEach(m -> IntStream.range(0, 5).forEach(__ -> m.effects.add(MARQUE)));})));
		cartes.add(new Carte<>(14, CarteType.POWER,1, 4, "Marques (x2)", Playable.monster((w, m) -> {IntStream.range(0, (int) m.effects.stream().filter(e -> e == MARQUE).count()).forEach(__ -> m.effects.add(MARQUE));})));
		cartes.add(new Carte<>(15, CarteType.POWER,2, 4, "x5 Attaques / Marque", Playable.monster((w, m) -> {w.attack(m, (int) m.effects.stream().filter(e -> e == MARQUE).count());})));



		return cartes;
	}
	
	
	public static void main(String[] args) throws IOException {
		
		MapWorld map = createMap();
		
		Heros heros = new Heros();
		heros.maxPv = 40;
		heros.pv = 30;
		heros.deck = shuffle(startDeck());
		
		World world = new World(map, heros);
		
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
			if (world.inChooseReward()) {
				int nextNode = interact.next(heros.hand.size());
				world.chooseReward(nextNode);
			}
			if (world.inFight()) {

				int nextNode = interact.next(heros.hand.size());

				if (nextNode == 0) {
					world.endTurn();
				}
				else {
					if (heros.hand.get(nextNode - 1).type == CarteType.OOPS) {
						continue;
					}
					if (heros.energy < heros.hand.get(nextNode - 1).cost) {
						continue;
					}
					Carte<?> carte = heros.hand.remove(nextNode - 1);
					world.play(carte);
					heros.played.add(carte);

					if (carte.action.type() == ContextType.MONSTER) {
						int monsterNode = interact.next(world.monsters.size() - 1);

						world.play(__ -> world.monsters.get(monsterNode));
					}
				}
			}

		}
	}

	private static MapWorld createMap() {
		List<List<Node>> all = generateNodes();

		Map<Node, List<Node>> links = generatePath(all);
		differentiateNode(all, links);
		
		
		Node start = new Node("Start", 0).type(NodeType.START);
		Node end = new Node("End", 0).type(NodeType.END);
		links.put(start, new ArrayList<>(all.get(0)));
		links.put(all.get(all.size() - 1).get(0), List.of(end));
		
		MapWorld map = new MapWorld();
		map.all = all;
		map.links = links;
		map.start = start;
		return map;
	}

	private static void displayMap(List<List<Node>> all, Map<Node, List<Node>> links) {
		displayMap(all, links, Collections.emptyList(), Collections.emptyList(), "ex1");
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
		
		//all.stream().flatMap(List::stream).forEach(n -> linksSource.add(node(n.title).with(attr("weight", random(8)+1))));
		
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
			linksSource.add(node(all.get(0).get(i).title).link(to(node(all.get(0).get(i + 1).title)).with(Color.RED).with(attr("weight", 1))));
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

	private static void differentiateNode(List<List<Node>> all, Map<Node, List<Node>> links) {
		List<Node> inside = all.stream().skip(1).flatMap(List::stream).filter(x -> x.type == NodeType.MONSTER).collect(Collectors.toList());
		
		List<Node> starting = all.get(0);
		
		boolean allPathHasShop = false;
		while (!allPathHasShop) {
			inside.get(random(inside.size())).type = NodeType.SHOP;
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
			
			Node n = inside.get(random(inside.size()));
			while (n.type != NodeType.MONSTER) {
				n = inside.get(random(inside.size()));
			}
			n.type = NodeType.ELITE;
		}
		
		for (int i = 0; i < 4; i++) {
			
			Node n = inside.get(random(inside.size()));
			while (n.type != NodeType.MONSTER) {
				n = inside.get(random(inside.size()));
			}
			n.type = NodeType.REPOS;
		}
		for (int i = 0; i < 8; i++) {
			
			Node n = inside.get(random(inside.size()));
			while (n.type != NodeType.MONSTER) {
				n = inside.get(random(inside.size()));
			}
			n.type = NodeType.ENIGME;
		}
		all.get(all.size()-2).forEach(x -> x.type = NodeType.REPOS);
	}

	private static List<List<Node>> generateNodes() {
		List<List<Node>> all = new ArrayList<>();
		int steps = 12;
		all.add(List.of(new Node("A", 0), new Node("B", 1), new Node("C", 2)));
		int n = 0;
		for (int i = 0; i < steps; i++) {
			int nbNode = random(4) + 2;
			List<Node> nodes = new ArrayList<>();
			for (int j = 0; j < nbNode; j++) {
				nodes.add(new Node((++n)+"", j));
			}
			all.add(nodes);
		}
		all.add(List.of(new Node("D", 0), new Node("E", 1), new Node("F", 2)));
		Node finalBoss = new Node("X", 0);
		finalBoss.type = NodeType.ELITE;
		all.add(List.of(finalBoss));
		all.get(steps / 2).forEach(x -> x.type = NodeType.COFFRE);
		return all;
	}

	private static Map<Node, List<Node>> generatePath(List<List<Node>> all) {
		Map<Node, Set<Node>> links = new HashMap<>();
		Set<Node> allNode = all.stream().flatMap(List::stream).collect(Collectors.toSet());
		while (!allNode.isEmpty()) {
			int fromLayer = random(all.size()-1);
			int from = random(all.get(fromLayer).size());
			int to = random(all.get(fromLayer + 1).size());
			
			Node nodeFrom = all.get(fromLayer).get(from);
			Node nodeTo = all.get(fromLayer + 1).get(to);
			
			if (!allNode.contains(nodeFrom) && !allNode.contains(nodeTo)) {
				continue;
			}
			
			if (!allNode.contains(nodeFrom) || !allNode.contains(nodeTo)) {
				if (random(100) > 5)
					continue;
			}
			
			double r_min = (from - 1.) / all.get(fromLayer).size();
			double r_max = (from + 1.) / all.get(fromLayer).size();
			
			double x_min = ((double) to - 1.) / all.get(fromLayer + 1).size();
			double x_max = ((double) to + 1.) / all.get(fromLayer + 1).size();
			
			
			boolean possible = (x_min >= r_min && x_min <= r_max) || (x_max >= r_min && x_max <= r_max) || (random(100) >90);
			//boolean possible = true; //x >= r_min && x <= r_max;
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
						|| node.title.equals("A")|| node.title.equals("B")|| node.title.equals("C")) {
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
