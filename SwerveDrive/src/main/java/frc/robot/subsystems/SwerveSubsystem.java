package frc.robot.subsystems;

import com.ctre.phoenix.sensors.Pigeon2;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.kinematics.ChassisSpeeds;
import edu.wpi.first.math.kinematics.SwerveDriveKinematics;
import edu.wpi.first.math.kinematics.SwerveDriveOdometry;
import edu.wpi.first.math.kinematics.SwerveModulePosition;
import edu.wpi.first.math.kinematics.SwerveModuleState;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import frc.robot.Constants.SwerveKinematics;
import frc.robot.Constants.SwerveModuleConstants;

public class SwerveSubsystem extends SubsystemBase {

    // Instance of swerve modules, initalized with specific value
    private SwerveModule moduleOne = new SwerveModule(
            SwerveModuleConstants.SWERVE_MODULE_ONE_DRIVE,
            SwerveModuleConstants.SWERVE_MODULE_ONE_TURN,
            SwerveModuleConstants.SWERVE_MODULE_ONE_ENCODER,
            SwerveModuleConstants.SWERVE_MODULE_ONE_DRIVE_MOTOR_REVERSED,
            SwerveModuleConstants.SWERVE_MODULE_ONE_TURNING_MOTOR_REVERSED,
            SwerveModuleConstants.SWERVE_MODULE_ONE_ENCODER_OFFSET_RAD,
            SwerveModuleConstants.SWERVE_MODULE_ONE_ABSOLUTE_ENCODER_REVERSED);

    private SwerveModule moduleTwo = new SwerveModule(
            SwerveModuleConstants.SWERVE_MODULE_TWO_DRIVE,
            SwerveModuleConstants.SWERVE_MODULE_TWO_TURN,
            SwerveModuleConstants.SWERVE_MODULE_TWO_ENCODER,
            SwerveModuleConstants.SWERVE_MODULE_TWO_DRIVE_MOTOR_REVERSED,
            SwerveModuleConstants.SWERVE_MODULE_TWO_TURNING_MOTOR_REVERSED,
            SwerveModuleConstants.SWERVE_MODULE_TWO_ENCODER_OFFSET_RAD,
            SwerveModuleConstants.SWERVE_MODULE_TWO_ABSOLUTE_ENCODER_REVERSED);

    private SwerveModule moduleThree = new SwerveModule(
            SwerveModuleConstants.SWERVE_MODULE_THREE_DRIVE,
            SwerveModuleConstants.SWERVE_MODULE_THREE_TURN,
            SwerveModuleConstants.SWERVE_MODULE_THREE_ENCODER,
            SwerveModuleConstants.SWERVE_MODULE_THREE_DRIVE_MOTOR_REVERSED,
            SwerveModuleConstants.SWERVE_MODULE_THREE_TURNING_MOTOR_REVERSED,
            SwerveModuleConstants.SWERVE_MODULE_THREE_ENCODER_OFFSET_RAD,
            SwerveModuleConstants.SWERVE_MODULE_THREE_ABSOLUTE_ENCODER_REVERSED);

    private SwerveModule moduleFour = new SwerveModule(
            SwerveModuleConstants.SWERVE_MODULE_FOUR_DRIVE,
            SwerveModuleConstants.SWERVE_MODULE_FOUR_TURN,
            SwerveModuleConstants.SWERVE_MODULE_FOUR_ENCODER,
            SwerveModuleConstants.SWERVE_MODULE_FOUR_DRIVE_MOTOR_REVERSED,
            SwerveModuleConstants.SWERVE_MODULE_FOUR_TURNING_MOTOR_REVERSED,
            SwerveModuleConstants.SWERVE_MODULE_FOUR_ENCODER_OFFSET_RAD,
            SwerveModuleConstants.SWERVE_MODULE_FOUR_ABSOLUTE_ENCODER_REVERSED);

    // Instance of Pigeon2 (the gyro) on the specifc swerve CAN bus
    private Pigeon2 gyro = new Pigeon2(SwerveModuleConstants.GRYO_ID,
            SwerveModuleConstants.SWERVE_CAN_BUS);

    private SwerveDriveOdometry odometer = new SwerveDriveOdometry(SwerveKinematics.driveKinematics, new Rotation2d(0),
            getModulePositions());

    /**
     * Initializes a new SwerveSubsystem object, and zeros the heading after a delay
     * to allow the pigeon to turn on and load
     */
    public SwerveSubsystem() {
        new Thread(() -> {
            try {
                Thread.sleep(1000);
                zeroHeading();
            } catch (Exception e) {
            }
        });
    }

    /**
     * Zeros the heading of the robot - makes the direction it is facing zero
     */
    public void zeroHeading() {
        gyro.setYaw(0);
    }

    /**
     * Returns the current heading of the robot
     * 
     * @return current heading of the robot
     */
    public double getHeading() {
        return gyro.getYaw();
    }

    /**
     * Returns the current rotation information of the robot
     * 
     * @return current rotation of the robot
     */
    public Rotation2d getRotation2d() {
        return Rotation2d.fromDegrees(getHeading());
    }

    public SwerveModulePosition[] getModulePositions() {
        SwerveModulePosition[] positions = new SwerveModulePosition[] { this.moduleOne.getPosition(),
                this.moduleTwo.getPosition(), this.moduleThree.getPosition(), this.moduleFour.getPosition() };
        return positions;
    }

    public SwerveModuleState[] getModuleStates() {
        SwerveModuleState[] states = new SwerveModuleState[] { this.moduleOne.getState(),
                this.moduleTwo.getState(), this.moduleThree.getState(), this.moduleFour.getState() };
        return states;
    }

    public Pose2d getPose() {
        return odometer.getPoseMeters();
    }

    public void resetOdometry(Pose2d pose) {
        odometer.resetPosition(getRotation2d(), getModulePositions(), pose);
    }

    @Override
    public void periodic() {
        odometer.update(getRotation2d(), getModulePositions());

        SmartDashboard.putNumber("Robot Heading", getHeading());
        SmartDashboard.putString("Robot Location", getPose().getTranslation().toString());
    }

    /**
     * Stops all the swerve modules
     */
    public void stopModules() {
        this.moduleOne.stop();
        this.moduleTwo.stop();
        this.moduleThree.stop();
        this.moduleFour.stop();
    }

    public ChassisSpeeds getChassisSpeeds() {
        return SwerveKinematics.driveKinematics.toChassisSpeeds(getModuleStates());

    }

    public void setChasisSpeeds(ChassisSpeeds chassisSpeeds) {

        // Converts the chassis speeds to module states and sets them as the desired
        // ones for the modules
        SwerveModuleState[] moduleStates = SwerveKinematics.driveKinematics.toSwerveModuleStates(chassisSpeeds);
        setModuleStates(moduleStates);
    }

    /**
     * Sets the destired states to the correct swerve modules
     * 
     * @param desiredStates - states to be relayed to the swerve modules
     */
    public void setModuleStates(SwerveModuleState[] desiredStates) {
        SwerveDriveKinematics.desaturateWheelSpeeds(desiredStates,
                SwerveKinematics.PHYSICAL_MAX_SPEED_METERS_PER_SECOND);

        this.moduleOne.setDesiredState(desiredStates[0]);
        this.moduleTwo.setDesiredState(desiredStates[1]);
        this.moduleThree.setDesiredState(desiredStates[2]);
        this.moduleFour.setDesiredState(desiredStates[3]);
    }

    /**
     * Ouputs information of the current swerve system
     */
    public void outputEncoderValues() {
        this.moduleOne.outputEncoderPosition();
        this.moduleTwo.outputEncoderPosition();
        this.moduleThree.outputEncoderPosition();
        this.moduleFour.outputEncoderPosition();

        SmartDashboard.putNumber("Gyro degrees:", getRotation2d().getDegrees());
    }
}
