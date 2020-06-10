package core.components;

import core.properties.PropertyString;
import core.properties.PropertyVector2D;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import utilities.Pair;
import utilities.Utils;
import utilities.Vector2D;

import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.*;

import static core.CoreConstants.imgHash;
import static utilities.Utils.getNeighbourhood;

public class GridBoard<T> extends Component {

    private int width;  // Width of the board
    private int height;  // Height of the board

    private T[][] grid;  // 2D grid representation of this board
    private Class<T> typeParameterClass;  // Type of this class

    private GridBoard() {
        super(Utils.ComponentType.BOARD);
        typeParameterClass = (Class<T>) String.class;
    }

    private GridBoard(int width, int height, Class<T> typeParameterClass){
        super(Utils.ComponentType.BOARD);
        this.width = width;
        this.height = height;
        this.typeParameterClass = typeParameterClass;
        this.grid = (T[][])Array.newInstance(typeParameterClass, height, width);
    }

    public GridBoard(int width, int height, Class<T> typeParameterClass, T defaultValue){
        this(width, height, typeParameterClass);
        for (int x = 0; x < width; x++)
            Arrays.fill(grid[x], defaultValue);
    }

    public GridBoard(T[][] grid, Class<T> typeParameterClass){
        super(Utils.ComponentType.BOARD);
        this.width = grid[0].length;
        this.height = grid.length;
        this.grid = grid;
        this.typeParameterClass = typeParameterClass;
    }

    public GridBoard(T[][] grid, Class<T> typeParameterClass, int ID){
        super(Utils.ComponentType.BOARD, ID);
        this.width = grid[0].length;
        this.height = grid.length;
        this.grid = grid;
        this.typeParameterClass = typeParameterClass;
    }

    public GridBoard(GridBoard<T> orig){
        super(Utils.ComponentType.BOARD);
        this.width = orig.getWidth();
        this.height = orig.getHeight();
        this.grid = orig.grid.clone();
        this.typeParameterClass = orig.typeParameterClass;
    }

    /**
     * Get the width and height of this grid.
     */
    public int getWidth(){return width; }
    public int getHeight(){return height; }

    /**
     * Sets the element at position (x, y).
     * @param x - x coordinate in the grid.
     * @param y - y coordinate in the grid.
     * @param value - new value for this element.
     * @return - true if coordinates in bounds, false otherwise (and function fails).
     */
    public boolean setElement(int x, int y, T value){
        if (x >= 0 && x < width && y >= 0 && y < height) {
            grid[y][x] = value;
            return true;
        }
        else
            return false;
    }

    /**
     * Retrieves the element at position (x, y).
     * @param x - x coordinate in the grid.
     * @param y - y coordinate in the grid.
     * @return - element at (x,y) in the grid.
     */
    public T getElement(int x, int y)
    {
        if (x >= 0 && x < width && y >= 0 && y < height)
            return grid[y][x];
        return null;
    }

    /**
     * Retrieves the grid.
     * @return - 2D grid.
     */
    public T[][] getGridValues(){
        return grid;
    }

    /**
     * Returns a new grid, copy of this one, with given orientation.
     * @param orientation - int orientation, how many times it should be rotated clockwise
     * @return - new grid with the same elements and correct orientation.
     */
    public T[][] rotate(int orientation) {
        GridBoard<T> copy = copy();
        orientation %= 4;  // Maximum 4 sides to a grid
        for (int i = 0; i < orientation; i++) {
            copy.grid = rotateClockWise(copy.grid);
        }
        return copy.grid;
    }

    /**
     * Rotates a given grid clockwise, returning new one
     * @param original - original grid to rotate
     * @return rotated grid
     */
    private T[][] rotateClockWise(T[][] original) {
        final int M = original.length;
        final int N = original[0].length;
        T[][] grid = (T[][])Array.newInstance(typeParameterClass, N, M);
        for (int r = 0; r < M; r++) {
            for (int c = 0; c < N; c++) {
                grid[c][M-1-r] = original[r][c];
            }
        }
        return grid;
    }

    /**
     * Returns a 1D representation of this grid, one row after another
     * @return 1D flattened grid
     */
    public T[] flattenGrid() {
        int length = getHeight() * getWidth();
        T[] array = (T[])Array.newInstance(typeParameterClass, length);
        for (int i = 0; i < getHeight(); i++) {
            System.arraycopy(grid[i], 0, array, i * getWidth(), grid[i].length);
        }
        return array;
    }

    @Override
    public GridBoard<T> copy() {
        T[][] gridCopy = (T[][])Array.newInstance(typeParameterClass, getHeight(), getWidth());
        for (int i = 0; i < height; i++) {
            if (width >= 0) System.arraycopy(grid[i], 0, gridCopy[i], 0, width);
        }
        GridBoard<T> g = new GridBoard<>(gridCopy, typeParameterClass, componentID);
        copyComponentTo(g);
        return g;
    }

    @Override
    public String toString() {
        String s = "";
        for (int y = 0; y < getHeight(); y++) {
            for (int x = 0; x < getWidth(); x++) {
                T t = getElement(x, y);
                if (t instanceof Character && t.equals(' ')) s += '.';
                else s += t + " ";
            }
            s += "\n";
        }
        return s;
    }

    /**
     * Loads all boards from a JSON file.
     * @param filename - path to file.
     * @return - List of Board objects.
     */
    public static List<GridBoard> loadBoards(String filename)
    {
        JSONParser jsonParser = new JSONParser();
        ArrayList<GridBoard> gridBoards = new ArrayList<>();

        try (FileReader reader = new FileReader(filename)) {

            JSONArray data = (JSONArray) jsonParser.parse(reader);
            for(Object o : data) {
                GridBoard newGridBoard = new GridBoard();
                newGridBoard.loadBoard((JSONObject) o);
                gridBoards.add(newGridBoard);
            }

        }catch (IOException | ParseException e) {
            e.printStackTrace();
        }

        return gridBoards;
    }

    /**
     * Loads board info from a JSON file.
     * @param board - board to load in JSON format
     */
    public void loadBoard(JSONObject board) {
        componentName = (String) board.get("id");

        JSONArray size = (JSONArray) board.get("size");
        this.width = (int)(long)size.get(0);
        this.height = (int)(long)size.get(1);

        if (board.get("img") != null) {
            properties.put(imgHash, new PropertyString((String) board.get("img")));
        }

        String classType = ((String) board.get("class")).toLowerCase();
        if (classType.equals("string")) {
            typeParameterClass = (Class<T>) String.class;
        } else if (classType.equals("character")) {
            typeParameterClass = (Class<T>) Character.class;
        }  // TODO: others

        this.grid = (T[][])Array.newInstance(typeParameterClass, height, width);

        JSONArray grid = (JSONArray) board.get("grid");
        int y = 0;
        for (Object o : grid) {
            JSONArray row = (JSONArray) o;
            int x = 0;
            for (Object o1 : row) {
                setElement(x, y, (T) o1);
                x++;
            }
            y++;
        }
    }

    /**
     * Generates a graph from this grid, with 4-way or 8-way connectivity.
     * @param way8 - if true, the board has 8-way connectivity, otherwise 4-way.
     * @return - GraphBoard, board with board nodes connected. All board nodes have information about their location
     * in the original grid, via the "coordinates" property.
     */
    public GraphBoard toGraphBoard(boolean way8) {
        GraphBoard gb = new GraphBoard(componentName, componentID);
        HashMap<Vector2D, BoardNode> bnMapping = new HashMap<>();
        // Add all cells as board nodes connected to each other
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                BoardNode bn = new BoardNode(-1, getElement(j, i).toString());
                bn.setProperty(new PropertyVector2D("coordinates", new Vector2D(j, i)));
                gb.addBoardNode(bn);
                bnMapping.put(new Vector2D(j, i), bn);
            }
        }

        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                BoardNode bn = bnMapping.get(new Vector2D(j, i));

                // Add neighbours
                List<Vector2D> neighbours = getNeighbourhood(j, i, width, height, way8);
                for (Vector2D neighbour: neighbours) {
                    BoardNode bn2 = bnMapping.get(neighbour);
                    gb.addConnection(bn, bn2);
                }
            }
        }
        return gb;
    }

    public GraphBoard toGraphBoard(List<Pair<Vector2D,Vector2D>> neighbours, boolean way8) {
        GraphBoard gb = new GraphBoard(componentName, componentID);
        HashMap<Vector2D, BoardNode> bnMapping = new HashMap<>();
        // Add all cells as board nodes connected to each other
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                if (getElement(j, i) != null) {
                    BoardNode bn = new BoardNode(-1, getElement(j, i).toString());
                    bn.setProperty(new PropertyVector2D("coordinates", new Vector2D(j, i)));
                    gb.addBoardNode(bn);
                    bnMapping.put(new Vector2D(j, i), bn);
                }
            }
        }
        for (Pair<Vector2D, Vector2D> p: neighbours) {
            gb.addConnection(bnMapping.get(p.a), bnMapping.get(p.b));
        }
        return gb;
    }
}
