import java.awt.Point;
import java.util.ArrayList;
import java.util.HashMap;

public class PlayerAI extends ClientAI {
	CustomGameboard cboard;
	int turns = 0; //first turn is turn 0, you're MAKING turn 1;
	
	ArrayList<CustomGameboard> preselectionList;
	ArrayList<CustomGameboard> processingList;
	
	public final int TOLERANCE = 500; //tolerance of the greedy algorithm. The lower this number is, the faster it runs
	public final int MAX_DEPTH = 1; //the furthest you want to recurse a solution
	
	public PlayerAI() {
		//Write your initialization here
	}

	@Override
	public Move getMove(Gameboard gameboard, Opponent opponent, Player player) throws NoItemException, MapOutOfBoundsException {
		long startTime = System.nanoTime();

		turns++; //first turn is turn 0, you're MAKING turn 1;
		if (cboard == null) {
			cboard = new CustomGameboard(gameboard.getWidth(), gameboard.getHeight(),
				opponent, player, gameboard.getWalls(), gameboard.getTurrets(), gameboard.getPowerUps(), 
				gameboard.getTeleportLocations(), gameboard.getBullets());
		} else {
			cboard.updateCustomGameboard(opponent, player, gameboard.getTurrets(), 
					gameboard.getPowerUps(), gameboard.getBullets());
		}
		
		processingList = new ArrayList<CustomGameboard>();
		preselectionList = new ArrayList<CustomGameboard>();
		processingList.add(cboard);
		CustomGameboard workingBoard, maxBoard = cboard;
		int depth = 0;
		
		
		while (depth < MAX_DEPTH) {
			depth++;
			while (processingList.size() != 0 && (workingBoard = processingList.remove(0)) != null) {
				for (Move move : Move.values()) {
					cboard.checkMove(player.getHP(), move, turns);
					preselectionList.add(workingBoard.shallowClone());
				}
			}
			
			//find max of preselection list
			maxBoard = preselectionList.get(0);
			for (CustomGameboard b : preselectionList) {
				if (b.moveScore > maxBoard.moveScore) maxBoard = b;
			}
			
			//break if max depth reached
			//this prevents deep cloning unless necessary
			if (depth >= MAX_DEPTH) break;
			
			//add boards within tolerance of max score to processing queue after cloning
			while ((workingBoard = preselectionList.remove(0)) != null) {
				//add anything within tolerance to processing list
				if (maxBoard.moveScore - workingBoard.moveScore < TOLERANCE) {
					//deep clone workingBoard
					//apply move to clone
					//add to processing list
				}
			}
		}
		
		return maxBoard.move;
		
		/*
		
		long endTime = System.nanoTime();
		long duration = (endTime - startTime);
		if (duration/1000 > 2000) return Move.FACE_UP;
		return Move.FACE_DOWN;
		
		*/
	} 
	
	public enum StaticObject {
		EMPTY, WALL, TURRET, TELEPORTER;
	}

	public class CustomGameboard {
		public int debug = 0;
		
		//Static content, never needs to be changed between calls
		//board state
		public StaticObject[][] staticBoard;
		public int width, height;
		
		//point system
		public final int DEATH = -2500;
		public final int GET_HIT = -750;
		public final int KILL = 2500;
		public final int HIT = 750;
		public final int POWERUP = 200;
		public final int KILL_TURRET = 500;
		public final int MOVE = 1;
		public final int ILLEGAL = -1;
		public final Direction[] directions = {Direction.LEFT, Direction.RIGHT, Direction.UP, Direction.DOWN};
		
		
		//Dynamic content, needs to change for every instance and update of CustomGameBoard
		public int moveScore;
		public Move move;
		
		//turret data
		public ArrayList<Turret> turrets;
		public HashMap<String, ArrayList<Turret>> turretKillZone;
		public HashMap<String, Turret> turretLocations;
		
		//player data
		public Point opponent, player;
		public Direction opponentDirection, playerDirection;

		//bullet data
		public ArrayList<Point> bulletPositions;
		public HashMap<String, Direction> bulletDirectionMap;
		public HashMap<String, Boolean> bulletSourceMap; //0 if your own, 1 if your opponents
		
		//power up data
		public HashMap<String, PowerUp> powerUpMap;
		
		public CustomGameboard(int width, int height, Opponent opponent, Player player,
				ArrayList<Wall> walls, ArrayList<Turret> turrets, ArrayList<PowerUp> powerUps,
				ArrayList<Point> teleportLocations, ArrayList<Bullet> bullets) {
			this.width = width;
			this.height = height;
			this.staticBoard = new StaticObject[width][height];
			
			for (int i = 0; i < width; i++) {
				for (int j = 0; j < height; j++) {
					staticBoard[i][j] = StaticObject.EMPTY;
				}
			}
			for (Wall wall : walls) {
				staticBoard[wall.getX()][wall.getY()] = StaticObject.WALL;
			}
			for (Point teleportLocation : teleportLocations) {
				staticBoard[teleportLocation.x][teleportLocation.y] = StaticObject.TELEPORTER;
			}
			
			updateCustomGameboard(opponent, player, turrets, powerUps, bullets); //dynamic portion
		}
		
		private CustomGameboard(int width, int height, StaticObject[][] staticBoard, Move move, int moveScore) {
			this.width = width;
			this.height = height;
			this.staticBoard = staticBoard;
			this.move = move;
			this.moveScore = moveScore;
		}

		public void updateCustomGameboard(Opponent opponent, Player player,
				ArrayList<Turret> turrets, ArrayList<PowerUp> powerUps, ArrayList<Bullet> bullets) {
			this.player = new Point(player.getX(), player.getY());
			this.opponent = new Point(opponent.getX(), opponent.getY());
			this.playerDirection = player.getDirection();
			this.opponentDirection = opponent.getDirection();
			
			this.turretKillZone = new HashMap<String, ArrayList<Turret>>();
			this.turretLocations = new HashMap<String, Turret>();
			
			this.bulletPositions = new ArrayList<Point>();
			this.bulletDirectionMap = new HashMap<String, Direction>();
			this.bulletSourceMap = new HashMap<String, Boolean>();
			
			this.powerUpMap = new HashMap<String, PowerUp>();
			
			this.moveScore = 0;
			
			this.turrets = turrets;
			for (Turret turret : turrets) {
				turretLocations.put(turret.getX() + " " + turret.getY(), turret);
				
				//generate turret kill map
				//left 
				String key;
				int distance;
				Point turretPoint = new Point(turret.getX(), turret.getY());
				Point tempPoint;
				
				//generate turret map for each direction for each turret
				for (Direction d : directions) {
					distance = 0;
					tempPoint = movePoint(new Point(turretPoint), d);
					while(!isSolid(tempPoint) && distance <= 4) {
						key = tempPoint.x + " " + tempPoint.y;
						
						if (turretKillZone.get(key) == null) {
							turretKillZone.put(key, new ArrayList<Turret>());
						}
						turretKillZone.get(key).add(turret);
						
						movePoint(tempPoint, d);
						distance++;
					}
				}
			}
			
			for (PowerUp powerUp : powerUps) {
				powerUpMap.put(powerUp.getX() + " " + powerUp.getY(), powerUp);
			}
			
			for (Bullet bullet : bullets) {
				this.bulletPositions.add(new Point(bullet.getX(), bullet.getY()));
				this.bulletDirectionMap.put(bullet.getX() + " " + bullet.getY(), bullet.getDirection());
				this.bulletSourceMap.put(bullet.getX() + " " + bullet.getY(), bullet.getShooter() == opponent);
			}
		}
		
		public CustomGameboard shallowClone() {
			CustomGameboard clone = new CustomGameboard(width, height, staticBoard, move, moveScore);
			
			//shallow copy data
			
			//turret data
			clone.turrets = turrets;
			clone.turretKillZone = turretKillZone;
			clone.turretLocations = turretLocations;
			
			//player data
			clone.opponent = opponent;
			clone.player = player;
			clone.opponentDirection = opponentDirection;
			clone.playerDirection = playerDirection;

			//bullet data
			clone.bulletPositions = bulletPositions;
			clone.bulletDirectionMap = bulletDirectionMap;
			clone.bulletSourceMap = bulletSourceMap; //0 if your own, 1 if your opponents
			
			//power up data
			clone.powerUpMap = powerUpMap;
			
			return clone;
		}
		
		public CustomGameboard deepClone() {
			CustomGameboard clone = new CustomGameboard(width, height, staticBoard, move, moveScore);
			return clone;
		}
		
		public void checkMove(int hp, Move move, int turn) {
			this.move = move;
			Point newPlayer = new Point(player);
			if (move == Move.FORWARD) {
				newPlayer = movePoint(newPlayer, playerDirection);
				//check for death or illegal moves
				if (isSolid(newPlayer)) {
					moveScore += ILLEGAL;
					return;
				}
				//bullet and player collide head on
				if (bulletDirectionMap.get(newPlayer.x + " " + newPlayer.y) == opposite(playerDirection)) {
					moveScore += ((hp == 1 ? DEATH : GET_HIT) + MOVE);
					return;
				}
			}
			
			//Process turret death
			if (turretKillZone.get(newPlayer.x + " " + newPlayer.y) != null) 
				for (Turret turret : turretKillZone.get(newPlayer.x + " " + newPlayer.y)) 
					if (isTurretOn(turret, turn)) {
						moveScore += ((hp == 1 ? DEATH : GET_HIT) + (move == Move.FORWARD ? MOVE : 0));
						return;
					}
			
			//Process bullet death
			//IMPORTANT: IMPLEMENT OVERLAPPING BULLETS
			/*
			ArrayList<Point> newBulletPositions = new ArrayList<Point>();
			HashMap<String, Direction> newBulletDirectionMap = new HashMap<String, Direction>();
			HashMap<String, Boolean> newBulletSourceMap = new HashMap<String, Boolean>();
			String oldKey, newKey;
			for (Point bulletPosition : bulletPositions) {
				oldKey = pointToKey(bulletPosition);
				Point newPosition = new Point(bulletPosition);
				movePoint(newPosition, bulletDirectionMap.get(oldKey));
				newKey = pointToKey(newPosition);
				
				if (newPosition.equals(newPlayer)) return true;
				
				if (!isSolid(newPosition)) {
					newBulletPositions.add(newPosition);
					newBulletDirectionMap.put(pointToKey(newPosition), bulletDirectionMap.get(oldKey));
					newBulletSourceMap.put(pointToKey(newPosition), bulletSourceMap.get(oldKey));
				}
			}
			*/
			for (Point bulletPosition : bulletPositions) {
				Point newPosition = new Point(bulletPosition);
				movePoint(newPosition, bulletDirectionMap.get(pointToKey(bulletPosition)));
				
				if (newPosition.equals(newPlayer)) {
					moveScore += ((hp == 1 ? DEATH : GET_HIT) + (move == Move.FORWARD ? MOVE : 0));
					return;
				}
			}
			
			
			if (move == Move.FORWARD) {
				moveScore += MOVE;
				if (powerUpMap.get(pointToKey(newPlayer)) != null)
					moveScore += POWERUP;
				
				return;
			}
			
			if (move == Move.SHOOT) {
				if (checkKillTurret(newPlayer, playerDirection)) {
					moveScore += KILL_TURRET;
					return;
				}
			}
			
			//movescore incremented by 0
			return;
		}
		
		public boolean checkKillTurret(Point player, Direction playerDirection) {
			Point newPlayer = new Point(player);
			Turret t;
			while (!isSolid(movePoint(newPlayer, playerDirection)) && !newPlayer.equals(player)) {
				t = turretLocations.get(pointToKey(newPlayer));
				if (t != null && !t.isDead()) return true;
			}
			return false;
		}
		
		public boolean isTurretOn(Turret t, int turn) {
			if (t.isDead()) return false;
			
			int ft = t.getFireTime();
			int ct = t.getCooldownTime();
			
			return (turn - 1) % (ft + ct) < ft;
		}
		
		Direction opposite(Direction d) {
			if (d == Direction.DOWN) return Direction.UP;
			if (d == Direction.LEFT) return Direction.RIGHT;
			if (d == Direction.RIGHT) return Direction.LEFT;
			return Direction.DOWN;
		}
		
		public Point movePoint(Point p, Direction d) {
			if (d == Direction.UP) {
				p.y -= 1;
			} else if (d == Direction.RIGHT) {
				p.x += 1;
			} else if (d == Direction.DOWN) {
				p.y += 1;
			} else { //left
				p.x -= 1;
			}
			return wrapCoordinate(p);
		}
		
		public String pointToKey(Point p) {
			return p.x + " " + p.y;
		}
		
		public Point wrapCoordinate(Point p) {
			while (p.x < 0) p.x += width;
			while (p.y < 0) p.y += height;
					
			p.x %= width;
			p.y %= height;
			
			return p;
		}
		
		public Point wrapCoordinate(int x, int y) {
			return wrapCoordinate(new Point(x, y));
		}
		
		public boolean isSolid(Point p) {
			if (staticBoard[p.x][p.y] == StaticObject.WALL || staticBoard[p.x][p.y] == StaticObject.TURRET)
				return true;
			return false;
		}
		
	}

}
