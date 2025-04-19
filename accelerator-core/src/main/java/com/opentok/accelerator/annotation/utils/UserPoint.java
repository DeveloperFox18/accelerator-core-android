import android.graphics.PointF;

public class UserPoint {
    private PointF point;
    private UserType userType;

    public UserPoint(PointF point, UserType userType) {
        this.point = point;
        this.userType = userType;
    }

    public PointF getPoint() {
        return point;
    }

    public void setPoint(PointF point) {
        this.point = point;
    }

    public UserType getUserType() {
        return userType;
    }

    public void setUserType(UserType userType) {
        this.userType = userType;
    }

    @Override
    public String toString() {
        return "UserPoint{" +
                "point=" + point +
                ", userType=" + userType +
                '}';
    }
}
