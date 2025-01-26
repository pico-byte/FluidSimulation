public class Tile {
    /**Wall tile*/
    boolean wall;
    /**Pressure in cell*/
    double pressure;

     /**Vertical Vector component*/
    public double VY;
    /**Horizontal Vector component*/
    public double VX;
    /**Density of the cell*/
    public double density;

    /**Vertical Vector component from last iteration*/
    public double prevVY;
    /**Horizontal Vector component from last iteration*/
    public double prevVX;
    /**Density of the cell from last iteration*/
    public double prevdensity;




    /**Constructor with all values*/
    public Tile (double vUp, double vSide, double density) {
        this.VY = vUp;
        this.VX = vSide;
        this.density = density;
        this.prevVX = vSide;
        this.prevVY = vUp;
        this.prevdensity = density;
        this.wall =false;
        this.pressure = 0;
    }

    /**Empty Constructor*/
    public Tile() {
        this(0, 0, 0.01);
        this.wall = false;
    }
}

