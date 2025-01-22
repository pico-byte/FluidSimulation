import java.awt.*;

public class Tile {
    boolean wall;
    double pressure;

    public double VY;
    public double VX;
    public double density;

    public double prevVY;
    public double prevVX;
    public double prevdensity;
    boolean marked = false;





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

    public Tile() {
        this(0, 0, 0.01);
        this.wall = false;
    }
}

