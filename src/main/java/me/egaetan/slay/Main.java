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

		default void startTurn(Monster m, World w) {};
		
		default String description() {return "";};
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
	

	public static class Personnage {

		int pv;
		int force;
		int armure;
		int vulnerability;
		int faiblesse;

		List<Effects> effects = new ArrayList<>();

		
		public void addDefense(int d) {
			armure += d;
		}

		public void addForce(int f) {
			force += f;
		}

		public void attack(int a) {
			pv -= Math.max(0, a - armure) * (vulnerability > 0 ? 1.5 : 1.);
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
			tmp.forEach(c -> {if (!c.ephemere) deck.add(c);});
		}

		public void clearHand() {
			for (Carte<?> carte : new ArrayList<>(hand)) {
				clearCard(carte);
			}
			hand.clear();
		}

		public void clearCard(Carte<?> carte) {
			if (carte.type == CarteType.GAME) {
				return;
			}
			if (!carte.ethereal) {
				played.add(carte);
			}
			else {
				retired.add(carte);
			}
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
		List<Monster> monsters= new ArrayList<>();
		Map<Monster, MonsterBrain> intentions = new HashMap<>();
		
		private MapWorld map;
		private WorldDeck deck;
		private Heros hero;
		
		private Node position = null;
		
		private WorldState state;
		private WorldState contextState;

		private Carte<?> currentCard;
		private int cardsRemoved;
		
		List<BiConsumer<Carte<?>, World>> beforePlay = new ArrayList<>();
		List<BiConsumer<Carte<?>, World>> afterPlay = new ArrayList<>();
		
		List<IntUnaryOperator> attackStack = new ArrayList<>();
		List<ItemShop> shopItems = new ArrayList<>();
		private Playable<?> currentPlayable;
		private Runnable endTurn = () -> {};
		
		
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
				monsters = new ArrayList<>(createMonsters());
				startFight();
				startTurn();
			}
			if (position.type == NodeType.ELITE) {
				monsters = new ArrayList<>(createEliteMonsters());
				startFight();
				startTurn();
			}
			if (position.type == NodeType.SHOP) {
				startShop();
			}
		}

		public void startShop() {
			state = WorldState.Shop;
			shopItems = new ArrayList<>(createShop());
			endTurn = () -> {};
		}
		
		public void playChooseItem(ItemShop item) {
			if (item.price() <= hero.gold) {
				hero.gold -= item.price();
				Playable<?> bought = item.buy(this);
				currentPlayable = bought;
				currentPlayable.type().handle(this);
				endTurn.run();
			}
		}

		public <T> void play(Context<T> context) {
			state.next(this);
			@SuppressWarnings("unchecked")
			Playable<T> playable = (Playable<T>) currentPlayable;
			boolean play = playable.play(this, context);
			if (play) {
				currentPlayable = null;
				endTurn.run();
			}
		}
		
		private List<ItemShop> createReward() {
			List<Carte<?>> carteList = shuffle(deck.nextDeck());
			List<ItemShop> shop = new ArrayList<>();
			shop.add(new ItemShop() {
				

				@Override
				public int price() {
					return 0;
				}
				
				@Override
				public Carte<?> description() {
					return new Carte<Carte<?>>(-1, CarteType.GAME, 0, CarteClass.GAME, "Aucune", null);
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
			});
			
			for (int i = 0; i < 3; i++) {
				Carte<?> carte = carteList.get(i);
				shop.add(ItemShop.carte(0, carte));
			}
			
			return shop;
		}
		
		private List<ItemShop> createShop() {
			List<Carte<?>> carteList = shuffle(deck.nextDeck());
			List<ItemShop> shop = new ArrayList<>();
			shop.add(new ItemShop() {
				

				@Override
				public int price() {
					return 0;
				}
				
				@Override
				public Carte<?> description() {
					return new Carte<Carte<?>>(-1, CarteType.GAME, 0, CarteClass.GAME, "Quitter la boutique", null);
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
			});
			
			for (int i = 0; i < 6; i++) {
				Carte<?> carte = carteList.get(i);
				int price = (int) (carte.classe.ordinal() * 25 * (random(100) + 50) / 100.);
				shop.add(ItemShop.carte(price, carte));
			}
			shop.add(new ItemShop() {
				

				@Override
				public int price() {
					return 25 * (World.this.cardsRemoved + 1);
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
						w.hero.deck.add(deck.blob.carte());
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
						w.hero.deck.add(deck.blob.carte());
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
			List<Monster> guepes = List.of(reine, guepeSupplier.apply(0), guepeSupplier.apply(1), guepeSupplier.apply(2));
			
			List<Monster> marteau = List.of(new Monster("Sponge", 60, MonsterStrategy.prepareAndThensimpleStrategy(List.of(
					MonsterBrain.buff((m, w) -> {
						m.effects.add(new Effects() {
							@Override
							public void startTurn(Monster m, World w) {
								m.faiblesse = 0;
								m.force+=2;
							}
						});
					})
			), List.of(
					MonsterBrain.attack(10)
			))));
			
			var allMonsters = List.of(guepes, marteau);
			return allMonsters.get(random(allMonsters.size()));
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
			state = WorldState.Fight;
			shuffle(hero.deck);
			for (Carte<?> c : hero.deck) c.resetCost();
			intentions.clear();
			hero.vulnerability = 0;
			hero.faiblesse = 0;
			hero.armure = 0;
			hero.force = 0;
			endTurn = this::afterPlay;
			beforePlay.clear();
			afterPlay.clear();
		}
		
		public void startTurn() {
			hero.energy = hero.startEnergy ;
			hero.vulnerability = Math.max(0, hero.vulnerability - 1);
			hero.faiblesse = Math.max(0, hero.faiblesse - 1);
			hero.armure = 0;
			
			hero.hand.add(deck.endTurn());
			
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
			hero.clearHand();

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
			beforePlay.clear();
			afterPlay.clear();
			
			startChooseReward();
		}

		public void startChooseReward() {
			state = WorldState.ChoseReward;
			hero.winGold(random(25) + 25);


			shopItems = new ArrayList<>(createReward());
			endTurn = () -> World.this.state = WorldState.Map;
		}

		public void playCard(Carte<?> carte) {
			if (hero.energy >= carte.cost && carte.type != CarteType.UNPLAYABLE && hero.hand.remove(carte)) {
				hero.energy -= carte.cost;
				
				hero.clearCard(carte);
				
				this.currentCard = carte;
				this.currentPlayable = carte.action;

				beforePlay(carte);
				currentPlayable.type().handle(this);
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
	
	public static interface ItemShop {
		
		public static ItemShop carte(int price, Carte<?> carte) {
			return new ItemShop() {

				@Override
				public Playable<?> buy(World world) {
					return Playable.solo(w -> w.hero.deck.add(carte));
				}

				@Override
				public int price() {
					return price;
				}

				@Override
				public Carte<?> description() {
					return carte;
				}
				
			};
			
		}
		
		public Playable<?> buy(World world);

		public int price();
		
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
		
		String dpath = "m";

		public void resume(String action) {
			System.out.println(action);
		}

		public int next(int max) {
			{
				System.out.println(world.hero.pv + "/" + world.hero.maxPv + " Gold:" + world.hero.gold +  " armure:" + world.hero.armure + " force:" + world.hero.force + " faiblesse:" + world.hero.faiblesse + " vulnerabilité:" + world.hero.vulnerability);

				List<Monster> monstre = world.monsters;
				for (int j = 0; j < monstre.size(); j++) {
					System.out.println(monstre.get(j).name() + " / " + monstre.get(j).pv +"pv / "+ " force:" + monstre.get(j).force+ " armure:" + monstre.get(j).armure + " vulnerabilité:" + monstre.get(j).vulnerability + " faiblesse:" + monstre.get(j).faiblesse + " => " + world.intentions.get(monstre.get(j)).display());
				}
			}
			
			if (world.inMap()) {
				List<Node> nexts = world.map.links.get(world.position);
				visited.add(world.position);
				displayMap(world.map.all, world.map.links, nexts, visited, dpath);
				System.out.println("Map => "+dpath);
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
					System.out.println((j) + ") ["+ shopItems.get(j).price() +"] " + shopItems.get(j).description().name);
				}
			}
			if (world.inFight()) {
				List<Carte<?>> hand = world.hero.hand;
				System.out.println(world.hero.energy + "/" + world.hero.startEnergy);
				for (int j = 0; j < hand.size(); j++) {
					System.out.println((j) + ") "+ (hand.get(j).type != CarteType.GAME ? "["+ hand.get(j).cost +"] ":"") + hand.get(j).name);
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
			dpath += choice;
			System.out.println(" " + dpath);
			return choice;
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
		FORCE,
		ARMURE,
		VULNERABILITY,
		FAIBLESSE,
		;
	}
	
	static class ActionEffect {
		ActionEffectType type;
		
	}
	
	static class Action {
		Personnage origin;
		Personnage cible;
		
		List<ActionEffect> effects;
	}
	
	
	
	static class WorldDeck {
		
		private final List<CarteDeck> deck = new ArrayList<>();
		private final CarteDeck blob;
		private final CarteDeck blessure;
		private final Effects marque = new Effects() {};
		
		private final CarteDeck startAttack;
		private final CarteDeck startDefense;
		private final CarteDeck startForce;
		private final CarteDeck startVuln;
		
		private final CarteDeck endTurn;
		
		public WorldDeck() {
			
			this.endTurn = ethereal(4, CarteType.GAME,0, CarteClass.GAME, "Fin de tour", Playable.solo(World::endTurn));
			
			this.startAttack = carte(1, CarteType.ATTACK,1, CarteClass.START, "Basic Attack (6)", Playable.monster((w,m) -> w.attack(m, 6)));
			this.startDefense = carte(1, CarteType.ARMURE,1, CarteClass.START, "Basic Defense (5)", Playable.solo((w) -> w.hero.addDefense(5)));
			this.startForce = carte(2, CarteType.POWER,1, CarteClass.START, "Renforcement (+2 Force)", Playable.solo((w) -> w.hero.addForce(2)));
			this.startVuln = carte(2, CarteType.POWER,1, CarteClass.START, "Attaque Vicieuse (4, +2 Vulnerabilité)", Playable.monster((w,m) -> {w.attack(m, 4); m.vulnerability+=2;}));
			
			this.blob = ephemere(17, CarteType.UNPLAYABLE,0, CarteClass.MALEDICTION, "Blob", Playable.notPlayable(__ -> {}));
			this.blessure = ephemere(11, CarteType.UNPLAYABLE,0, CarteClass.MALEDICTION, "Blessure (1 PV)", Playable.notPlayable(w -> w.hero.pv -= 1));
			
			carte(3, CarteType.POWER,0, CarteClass.COMMON, "Double Attack (Next attack x2)", Playable.attackStack(CarteType.ATTACK.is(), i -> i*2));
			carte(4, CarteType.POWER,0, CarteClass.COMMON, "Furie (+2 Energy, -6PV)", Playable.solo(w -> {w.hero.pv -= 6; w.hero.energy += 3;}));
			carte(5, CarteType.POWER,1, CarteClass.COMMON, "Draw (draw two cards)", Playable.solo(w -> {w.hero.drawCard();w.hero.drawCard();}));
			carte(6, CarteType.ATTACK,2, CarteClass.COMMON, "Medium Attack (15)", Playable.monster((w,m) -> w.attack(m, 15)));
			carte(7, CarteType.POWER,2, CarteClass.COMMON, "Faiblesse (faiblesse +3) ", Playable.monster((w,m) -> m.faiblesse +=3));
			carte(8, CarteType.ATTACK,3, CarteClass.LEGEND, "Big Attack (25)", Playable.monster((w,m) -> w.attack(m, 25)));
			carte(9, CarteType.ARMURE,1, CarteClass.RARE, "Defense (7 + draw one card)", Playable.solo((w) -> {w.hero.addDefense(5); w.hero.drawCard();}));
			carte(10, CarteType.ARMURE,1, CarteClass.RARE, "Ninja (+5 armure par attaque)", Playable.solo(w -> {w.afterPlay.add((c,z)->{if (c.type == CarteType.ATTACK) {w.hero.armure+=5;};});}));
			carte(10, CarteType.ARMURE,1, CarteClass.RARE, "BlockAttack (attaque de l'armure)", Playable.monster((w, m) -> {w.attack(m, w.hero.armure);}));
			carte(12, CarteType.ATTACK,1, CarteClass.COMMON, "Sacrifice (Attack (10) + une blessure)", Playable.monster((w, m) -> {w.attack(m, 10); w.hero.deck.add(blessure.carte());}));
			carte(13, CarteType.POWER,1, CarteClass.COMMON, "Marques (+5 marques à tous)", Playable.solo((w) -> {w.monsters.forEach(m -> IntStream.range(0, 5).forEach(__ -> m.effects.add(marque)));}));
			carte(14, CarteType.POWER,1, CarteClass.COMMON, "Marques (x2)", Playable.monster((w, m) -> {IntStream.range(0, (int) m.effects.stream().filter(e -> e == marque).count()).forEach(__ -> m.effects.add(marque));}));
			carte(15, CarteType.POWER,2, CarteClass.COMMON, "x5 Attaques / Marque", Playable.monster((w, m) -> {w.attack(m, (int) m.effects.stream().filter(e -> e == marque).count());}));


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
		private CarteDeck ephemere(int __, CarteType type, int originCost, CarteClass classe, String description, Playable<?> action) {
			CarteDeck cd = new CarteDeck(deck.size(), originCost, type, classe, description, action, true, false);
			deck.add(cd);
			return cd;
		}
		private CarteDeck ethereal(int __, CarteType type, int originCost, CarteClass classe, String description, Playable<?> action) {
			CarteDeck cd = new CarteDeck(deck.size(), originCost, type, classe, description, action, false, true);
			deck.add(cd);
			return cd;
		}
		
	}
	
	
	
	public static void main(String[] args) throws IOException {
		
		MapWorld map = createMap();
		WorldDeck deck = new WorldDeck();
		
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
