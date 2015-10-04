import PlayerAI.CustomGameboard;
import PlayerAI.MinimaxNode;
import PlayerAI.MinimaxNodeType;

public class MinimaxNode {
		ArrayList<MinimaxNode> child_nodes;
		CustomGameboard cboard;
		private int alpha, beta;
		private MinimaxNodeType type;
		private Move bestMove, currentMove;
		private int value;
		private Opponent opponent;
		private Player player;
		private Move[] action_moves = 
			{   
				Move.NONE,
				Move.SHOOT,
				Move.FORWARD 
			};
		private Move[] orientation_moves =
			{   
				Move.FACE_UP, 
				Move.FACE_DOWN,
				Move.FACE_LEFT,
				Move.FACE_RIGHT
			};
		//private Object MAP CHANGES
		
		MinimaxNode(CustomGameboard cboard, Move currentMove, Opponent opponent, Player player, MinimaxNodeType type) {
			this(cboard, currentMove, opponent, player, type, Integer.MIN_VALUE, Integer.MAX_VALUE);
		}
		
		MinimaxNode(CustomGameboard cboard, Move currentMove, Opponent opponent, Player player, MinimaxNodeType type, int alpha, int beta) {
			this.alpha = alpha;
			this.beta = beta;
			this.cboard = cboard;
			this.type = type;
			this.currentMove = currentMove;
			this.opponent = opponent;
			this.player = player;
		}
		
		public Move solve(int depth) {
			if (depth == 0) {
				//evaluate score
			}
			
			//create child nodes 	
			for (Move move : action_moves) {
				/*
				if (type == MinimaxNodeType.MIN) {
					child_nodes.add(new MinimaxNode(cboard, move, opponent, 
							new Player(player.getX(), player.getY(), player.getDirection(), 0, player.getHP(), 0, 0, 0, false, null, null, false), 
							MinimaxNodeType.MAX, this.alpha, this.beta));
				else
					child_nodes.add(new MinimaxNode(cboard, move, 
							new Opponent(opponent.getX(), opponent.getY(), null, , depth, move, false, depth, depth, depth), 
							player, MinimaxNodeType.MAX, this.alpha, this.beta));
				*/
			}
			if (currentMove != Move.FACE_UP && currentMove != Move.FACE_RIGHT && 
					currentMove != Move.FACE_DOWN && currentMove != Move.FACE_LEFT) {
				for (Move move : orientation_moves) {
					
				}		
			}
			//recurse down child nodes
			for (MinimaxNode child_node : child_nodes) {
				child_node.solve(depth - 1);
				if (this.type == MinimaxNodeType.MIN)
					this.beta = child_node.getValue();
				else
					this.alpha = child_node.getValue();
				
				
				
			}
			return null;
		}
		
		public Move getMove() {
			return bestMove;
		}
		
		public int getValue() {
			return value;
		}
		
		public int getAlpha() {
			return alpha;
		}
		
		public int getBeta() {
			return beta;
		}
	}
}
