import java.awt.Point;
import java.util.ArrayList;
import java.util.HashMap;

public class PlayerAI extends ClientAI {
	CustomGameboard cboard;
	int turns = 0; //first turn is turn 0, you're MAKING turn 1;
	
	ArrayList<CustomGameboard> preselectionList;
	ArrayList<CustomGameboard> processingList;
	
	public final Move[] VALID_MOVES = { 
			Move.FACE_UP,
			Move.FACE_DOWN,
			Move.FACE_LEFT,
			Move.FACE_RIGHT,
			Move.SHOOT,
			Move.FORWARD,
			Move.LASER
	};
	
	public final int TOLERANCE = 400; //tolerance of the greedy algorithm. The lower this number is, the faster it runs
	public int MAX_DEPTH = 4; //the furthest you want to recurse a solution
	public long ctime = 0;
	
	public PlayerAI() {
		//Write your initialization here
	}

	@Override
	public Move getMove(Gameboard gameboard, Opponent opponent, Player player) throws NoItemException, MapOutOfBoundsException {
		long startTime = System.nanoTime();
		turns++; //first turn is turn 0, you're MAKING turn 1;
		if (ctime < 5 && MAX_DEPTH < 6) MAX_DEPTH++;
		
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
		CustomGameboard workingBoard, tempBoard, maxBoard = cboard;
		int depth = 0;
		
		while (depth < MAX_DEPTH) {
			depth++;
			while (processingList.size() != 0 && (workingBoard = processingList.remove(0)) != null) {
				for (Move move : VALID_MOVES) {
					tempBoard = workingBoard.shallowClone();
					if (depth == 1) tempBoard.firstMove = move;
					tempBoard.checkMove(player.getHP(), move, turns + depth - 1);
					if (depth == 1) tempBoard.firstScore = tempBoard.moveScore;
					preselectionList.add(tempBoard);
				}
			}
				
			//find max of preselection list
			maxBoard = preselectionList.get(0);
			for (CustomGameboard b : preselectionList) {
				if (b.moveScore > maxBoard.moveScore || (b.moveScore == maxBoard.moveScore && b.firstScore > maxBoard.firstScore)) maxBoard = b;
			}
			
		
			//break if max depth reached
			//this prevents deep cloning unless necessary
			if (depth >= MAX_DEPTH) break;
			
			//add boards within tolerance of max score to processing queue after cloning
			while (preselectionList.size() != 0 && (workingBoard = preselectionList.remove(0)) != null) {
				//add anything within tolerance to processing list
				if (maxBoard.moveScore - workingBoard.moveScore < TOLERANCE) {
					//deep clone workingBoard
					tempBoard = workingBoard.deepClone();
					
					//apply move to clone
					tempBoard.applyMove();
					
					//add to processing list
					processingList.add(tempBoard);
				}
			}
		}
		
		long endTime = System.nanoTime();
		ctime = ((endTime - startTime)/1000000);
		
		if (maxBoard.firstScore < -500 && player.getShieldCount() != 0) 
			return Move.SHIELD;
		
		//System.out.println("First score: " + maxBoard.firstScore + ", Final Score: " + maxBoard.moveScore);
		
		//System.out.println(ctime);
		return maxBoard.firstMove;
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
		public final int ILLEGAL = -10000;
		public final int DANGER1 = -500;
		public final int DANGER2 = -100;
		public final Direction[] directions = {Direction.LEFT, Direction.RIGHT, Direction.UP, Direction.DOWN};
		
		
		//Dynamic content, needs to change for every instance and update of CustomGameBoard
		public int firstScore;
		public int moveScore;
		public Move firstMove;
		public Move currentMove;
		
		//public ArrayList<Integer> scores;
		//public ArrayList<Move> moves;
		
		//turret data
		public ArrayList<Turret> turrets;
		public HashMap<String, ArrayList<Turret>> turretKillZone;
		public HashMap<String, Turret> turretLocations;
		public HashMap<String, Boolean> turretOffline;
		
		//player data
		public Point opponent, player;
		public Direction opponentDirection, playerDirection;
		public Opponent opponentObj;
		public Player playerObj;

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
			for (Turret turret : turrets) {
				staticBoard[turret.getX()][turret.getY()] = StaticObject.TURRET;
			}
			
			updateCustomGameboard(opponent, player, turrets, powerUps, bullets); //dynamic portion
		}
		
		private CustomGameboard(int width, int height, StaticObject[][] staticBoard, Move firstMove, Move currentMove, int firstScore, int moveScore) {
			this.width = width;
			this.height = height;
			this.staticBoard = staticBoard;
			this.firstMove = firstMove;
			this.currentMove = currentMove;
			this.moveScore = moveScore;
			this.firstScore = firstScore;
		}

		public void updateCustomGameboard(Opponent opponent, Player player,
				ArrayList<Turret> turrets, ArrayList<PowerUp> powerUps, ArrayList<Bullet> bullets) {
			this.player = new Point(player.getX(), player.getY());
			this.opponent = new Point(opponent.getX(), opponent.getY());
			this.playerObj = player;
			this.opponentObj = opponent;
			this.playerDirection = player.getDirection();
			this.opponentDirection = opponent.getDirection();
			
			this.turretKillZone = new HashMap<String, ArrayList<Turret>>();
			this.turretLocations = new HashMap<String, Turret>();
			this.turretOffline = new HashMap<String, Boolean>();
			
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
				Point tempPoint;
				Point turretPoint = new Point(turret.getX(), turret.getY());
				
				if (turret.isDead()) turretOffline.put(pointToKey(turretPoint), true);
				
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
			CustomGameboard clone = new CustomGameboard(width, height, staticBoard, firstMove, currentMove, firstScore, moveScore);
			
			//shallow copy data
			
			//turret data
			clone.turrets = turrets;
			clone.turretKillZone = turretKillZone;
			clone.turretLocations = turretLocations;
			clone.turretOffline = new HashMap<String, Boolean>();
			for (String key : turretOffline.keySet()) {
				clone.turretOffline.put(key, turretOffline.get(key));
			}
			
			//player data
			clone.opponent = opponent;
			clone.player = player;
			clone.opponentObj = opponentObj;
			clone.playerObj = playerObj;
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
			CustomGameboard clone = new CustomGameboard(width, height, staticBoard, firstMove, currentMove, firstScore, moveScore);
			
			//deep copy data
			
			//turret data
			clone.turrets = turrets; // no need to deep clone this
			clone.turretKillZone = turretKillZone; //assume this does not change, since
				//it takes a few turns to kill a turret
			clone.turretLocations = turretLocations; 
			clone.turretOffline = new HashMap<String, Boolean>();
			for (String key : turretOffline.keySet()) {
				clone.turretOffline.put(key, turretOffline.get(key));
			}
			
			//player data
			clone.opponent = new Point(opponent);
			clone.player = new Point(player);
			clone.opponentObj = opponentObj;
			clone.playerObj = playerObj;
			clone.opponentDirection = opponentDirection; //the original direction values are never changed
			clone.playerDirection = playerDirection;

			//bullet data
			clone.bulletDirectionMap = new HashMap<String, Direction>();
			clone.bulletSourceMap = new HashMap<String, Boolean>();
			clone.bulletPositions = new ArrayList<Point>();
			String key;
			for (Point bulletPosition : bulletPositions) {
				key = pointToKey(bulletPosition);
				clone.bulletPositions.add(new Point(bulletPosition));
				clone.bulletDirectionMap.put(key, bulletDirectionMap.get(key));
				clone.bulletSourceMap.put(key, bulletSourceMap.get(key));
			}
			
			//power up data
			clone.powerUpMap = new HashMap<String, PowerUp>();
			for (String powerUpKey : powerUpMap.keySet())
				clone.powerUpMap.put(powerUpKey, powerUpMap.get(powerUpKey));
			
			return clone;
		}
		
		public void applyMove() {
			Move move = currentMove;
			if (move == Move.FACE_DOWN || move == Move.FACE_LEFT || move == Move.FACE_UP || move == Move.FACE_RIGHT) {
				Direction d = Move.moveToDirection(move);
				playerDirection = d;
			} else if (move == Move.FORWARD) {
				movePoint(player, playerDirection);
				powerUpMap.put(pointToKey(player), null);
			} else if (move == Move.SHOOT) {
				bulletPositions.add(player);
				bulletDirectionMap.put(pointToKey(player), playerDirection);
				bulletSourceMap.put(pointToKey(player), false);
			} 
			
			//update bullet positions
			ArrayList<Point> newBulletPositions = new ArrayList<Point>();
			HashMap<String, Direction> newBulletDirectionMap = new HashMap<String, Direction>();
			HashMap<String, Boolean> newBulletSourceMap = new HashMap<String, Boolean>();
			String oldKey, newKey;
			for (Point bulletPosition : bulletPositions) {
				oldKey = pointToKey(bulletPosition);
				Point newPosition = new Point(bulletPosition);
				movePoint(newPosition, bulletDirectionMap.get(oldKey));
				
				if (!isSolid(newPosition)) {
					newKey = pointToKey(newPosition);
					newBulletPositions.add(newPosition);
					newBulletDirectionMap.put(newKey, bulletDirectionMap.get(oldKey));
					newBulletSourceMap.put(newKey, bulletSourceMap.get(oldKey));
				}
			}
			bulletPositions = newBulletPositions;
			bulletDirectionMap = newBulletDirectionMap;
			bulletSourceMap = newBulletSourceMap;
		}
		
		public void checkMove(int hp, Move move, int turn) {
			this.currentMove = move;
			Point newPlayer = new Point(player);
			if (move == Move.FORWARD) {
				newPlayer = movePoint(newPlayer, playerDirection);
				//check for death by bullet collision
				if (bulletDirectionMap.get(newPlayer.x + " " + newPlayer.y) == opposite(playerDirection)) {
					moveScore += ((hp == 1 ? DEATH : GET_HIT) + MOVE);
					return;
				}
			}
			
			//Process death by turret
			if (turretKillZone.get(pointToKey(newPlayer)) != null) {
				for (Turret turret : turretKillZone.get(pointToKey(newPlayer))) {
					if (isTurretOn(turret, turn)) {
						moveScore += ((hp == 1 ? DEATH : GET_HIT) + (move == Move.FORWARD ? MOVE : 0));
						return;
					}
				}
			}
			
			for (Point bulletPosition : bulletPositions) {
				Point newPosition = new Point(bulletPosition);
				movePoint(newPosition, bulletDirectionMap.get(pointToKey(bulletPosition)));
				
				if (newPosition.equals(newPlayer)) {
					moveScore += ((hp == 1 ? DEATH : GET_HIT) + (move == Move.FORWARD ? MOVE : 0));
					return;
				}
			}
			
			//HIGH DANGER
			//laser danger
			if (opponentObj.getLaserCount() != 0) {
				if (move == Move.FORWARD) {
					Point front = new Point(newPlayer);
					movePoint(front, playerDirection);
					if (laserRange(opponent, front)) {
						moveScore += (hp == 1 ? DEATH : GET_HIT);
						return;
					}
				} else {
					if (laserRange(opponent, newPlayer)) {
						moveScore += (hp == 1 ? DEATH : GET_HIT);
						return;
					}
				}
			}
			
			//bullet danger
			Point opponentFront = new Point(opponent);
			movePoint(opponentFront, opponentDirection);
			if (opponentFront.equals(newPlayer)) {
				moveScore += DANGER1;
				return;
			}
			
			if (playerObj.getLaserCount() != 0 && move == Move.LASER && laserRange(newPlayer, opponent)) {
				moveScore += (opponentObj.getHP() == 1 ? KILL : HIT);
				return;
			}
			
			if (move == Move.SHOOT) {
				if (checkKillTurret(newPlayer, playerDirection)) {
					moveScore += KILL_TURRET;
					return;
				}
				
				Point front = new Point(newPlayer);
				movePoint(front, playerDirection);
				if (front.equals(opponent) || front.equals(opponentFront)) {
					moveScore += (HIT * 0.5);
					return;
				}
				movePoint(front, playerDirection);
				if (front.equals(opponent) || front.equals(opponentFront)) {
					moveScore += (HIT * 0.25);
					return;
				}
			}
			
			if (isSolid(newPlayer)) {
				moveScore += ILLEGAL;
				return;
			}
			
			if (move == Move.FACE_DOWN || move == Move.FACE_UP || move == Move.FACE_LEFT || move == Move.FACE_RIGHT) {
				if (Move.moveToDirection(move) == playerDirection) {
					moveScore += ILLEGAL;
					return;
				}
			}
			
			if (move == Move.FORWARD) {
				moveScore += MOVE;
				if (powerUpMap.get(pointToKey(newPlayer)) != null)
					moveScore += POWERUP;
				
				return;
			}
			
			//movescore incremented by 0
			return;
		}
		
		public boolean laserRange(Point source, Point target) {
			if (source.x != target.x && source.y != target.y) return false;
			Point tempPoint;
			int distance;
			//generate laser map for each direction for each turret
			for (Direction d : directions) {
				distance = 0;
				tempPoint = movePoint(new Point(source), d);
				while(!isSolid(tempPoint) && distance <= 4) {
					if (tempPoint.equals(target))
						return true;
						
					movePoint(tempPoint, d);
					distance++;
				}
			}
			return false;
		}
		
		public boolean checkKillTurret(Point player, Direction playerDirection) {

			Point newPlayer = new Point(player);
			Turret t;
			
			while (!isSolid(movePoint(newPlayer, playerDirection)) && !newPlayer.equals(player));
			
			t = turretLocations.get(pointToKey(newPlayer));
			if (t != null && turretOffline.get(pointToKey(newPlayer)) == null) {
				turretOffline.put(pointToKey(newPlayer), true);
				return true;
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


