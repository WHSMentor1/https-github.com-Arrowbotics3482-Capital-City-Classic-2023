package frc.robot.subsystems;

import com.ctre.phoenix.sensors.AbsoluteSensorRange;
import com.ctre.phoenix.sensors.CANCoder;
import com.ctre.phoenix.sensors.CANCoderConfiguration;
import com.ctre.phoenix.sensors.SensorTimeBase;
import com.revrobotics.CANSparkMax;
import com.revrobotics.CANSparkMax.IdleMode;
import com.revrobotics.CANSparkMaxLowLevel.MotorType;

import edu.wpi.first.math.controller.PIDController;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.kinematics.SwerveModulePosition;
import edu.wpi.first.math.kinematics.SwerveModuleState;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import frc.robot.Constants.SwerveKinematics;
import frc.robot.Constants.SwerveModuleConstants;

public class SwerveModule {
    // Instances of the drivng and turning motor for the module
    private CANSparkMax driveMotor;
    private CANSparkMax turningMotor;

    // Instance of the encoder used for turning of the module
    private CANCoder turningEncoder;

    // Instance of PIDcontroller - used to calculate value for turning motor
    private PIDController turningPidController;

    // Instances of values used to help calculate encoder positions for the turning
    // motor
    private boolean absoluteEncoderReversed;
    private double absoluteEncoderOffsetRad;

    /**
     * Creates an Instance of the Swerve Module with the specified options
     * 
     * @param driveMotorID            - CAN ID for the driving SparkMax
     * @param turningMotorID          - CAN ID for the turning SparkMax
     * @param turningEncoderID        - CAN ID for the turning encoder (on the
     *                                swerve CAN bus)
     * @param driveMotorReversed      - is the driving motor inverted?
     * @param turningMotorReversed    - is the turning motor inverted?
     * @param absoluteEcoderOffset    - absolute offset for the turning encoder
     * @param absoluteEncoderReversed - is the turning encoder inverted?
     */
    public SwerveModule(int driveMotorID, int turningMotorID, int turningEncoderID, boolean driveMotorReversed,
            boolean turningMotorReversed, double absoluteEcoderOffset,
            boolean absoluteEncoderReversed) {
        this.absoluteEncoderOffsetRad = absoluteEcoderOffset;
        this.absoluteEncoderReversed = absoluteEncoderReversed;

        // Initializes the driving and turning motor
        this.driveMotor = new CANSparkMax(driveMotorID, MotorType.kBrushless);
        this.driveMotor.setIdleMode(IdleMode.kBrake);
        this.driveMotor.setInverted(driveMotorReversed);

        this.turningMotor = new CANSparkMax(turningMotorID, MotorType.kBrushless);
        this.turningMotor.setIdleMode(IdleMode.kBrake);
        this.turningMotor.setInverted(turningMotorReversed);

        // Initializes the turning encoder on the specific CAN bus
        this.turningEncoder = new CANCoder(turningEncoderID, SwerveModuleConstants.SWERVE_CAN_BUS);

        // Initializes a new CANcoder config to make sure the CANcoder is outputing the
        // values how the module needs them (radians/second + angles from (-180 to 180))
        CANCoderConfiguration config = new CANCoderConfiguration();
        config.sensorCoefficient = 2 * Math.PI / 4096.0;
        config.unitString = "rad";
        config.sensorTimeBase = SensorTimeBase.PerSecond;
        config.absoluteSensorRange = AbsoluteSensorRange.Signed_PlusMinus180;

        this.turningEncoder.configAllSettings(config);

        // Initializes the PID controller using the determined values
        this.turningPidController = new PIDController(SwerveKinematics.KP, SwerveKinematics.KI,
                SwerveKinematics.KD);
        // Makes the values continueous, so that 0 == 360 degrees
        this.turningPidController.enableContinuousInput(-Math.PI, Math.PI);

        this.resetEncoders();
    }

    /**
     * Returns the turning position of the CANcoder
     * 
     * @return position of the turning CANcoder (in radians)
     */
    public double getTurningPosition() {
        return getAbsoluteEncoderRad();
    }

    /**
     * Returns the speed of the turning encoder
     * 
     * @return speed of the turning encoder (radians/second)
     */
    public double getTurningVelocity() {
        return this.turningEncoder.getVelocity();
    }

    /**
     * Returns the turning position of the CANcoder
     * 
     * @return position of the turning CANcoder (in radians)
     */
    public double getAbsoluteEncoderRad() {
        double angle = this.turningEncoder.getAbsolutePosition();
        angle -= this.absoluteEncoderOffsetRad;
        return angle * (this.absoluteEncoderReversed ? -1.0 : 1.0);
    }

    /*
     * Resets the encoder position to absolute encoder position
     */
    public void resetEncoders() {
        this.turningEncoder.setPosition((getAbsoluteEncoderRad()));
    }

    /**
     * Returns the current state of the swerve module
     * 
     * @return current state of the swerve module
     */
    public SwerveModuleState getState() {
        return new SwerveModuleState(this.driveMotor.getEncoder().getVelocity(),
                new Rotation2d((getTurningPosition())));
    }

    public SwerveModulePosition getPosition() {
        return new SwerveModulePosition(this.driveMotor.getEncoder().getPosition(),
                new Rotation2d(getTurningPosition()));
    }

    /**
     * Sets the current state to a desired state
     * 
     * @param state - desired state of the swerve module
     */
    public void setDesiredState(SwerveModuleState state) {
        if (Math.abs(state.speedMetersPerSecond) < 0.001) {
            // this.stop();
            // return;
        }
        state = SwerveModuleState.optimize((state), getState().angle);

        double driveMotorSpeed = state.speedMetersPerSecond
                / SwerveKinematics.PHYSICAL_MAX_SPEED_METERS_PER_SECOND;

        double turnMotorSpeed = turningPidController.calculate(getTurningPosition(), state.angle.getRadians());

        driveMotor.set(driveMotorSpeed);
        turningMotor.set(turnMotorSpeed);
    }

    /**
     * stops the swerve module by stopping both the turning and driving motor
     */
    public void stop() {
        this.driveMotor.set(0);
        this.turningMotor.set(0);
    }

    /**
     * Outputs information of the swerve module
     */
    public void outputEncoderPosition() {

        SmartDashboard.putNumber("Swerve[" + this.turningEncoder.getDeviceID() + "] encoder degrees:",
                this.getState().angle.getDegrees());
    }
}
