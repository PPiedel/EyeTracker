import java.io.ByteArrayInputStream;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.objdetect.Objdetect;
import org.opencv.videoio.VideoCapture;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import static org.opencv.imgproc.Imgproc.*;


public class FaceDetectionController {
    private final int framesPerSecond = 33;
    private final int initialDelay = 0;

    @FXML
    private Button cameraButton;
    // the FXML area for showing the current frame
    @FXML
    private ImageView originalFrame;

    // a timer for acquiring the video stream
    private ScheduledExecutorService timer;
    // the OpenCV object that performs the video capture
    private VideoCapture capture;
    // a flag to change the button behavior
    private boolean cameraActive;

    // face cascade classifier
    private CascadeClassifier faceCascade ;
    // Classifiers for left-right eyes
    private CascadeClassifier   lefEyeClassifier;
    private CascadeClassifier   rightEyeClassifier;

    private Mat frame;
    private Mat grayFrame;

    // Helper Mat
    private Mat mResult;
    // match value
    private double match_value;
    private int absoluteFaceSize;
    // rectangle used to extract eye region - ROI
    private Rect eyearea = new Rect();
    // counter of learning frames
    private int learn_frames = 0;
    // Mat for templates
    private Mat rightEyeTemplate;
    private Mat leftEyeTemplate;

    /**
     * Init the controller, at start time
     */
    protected void init() {
        this.capture = new VideoCapture();
        this.faceCascade = new CascadeClassifier("L:\\Studia\\ProgrammingProjects\\EyeTrackerNEW\\src\\main\\resources\\haarcascade_frontalface_alt.xml");
        lefEyeClassifier = new CascadeClassifier("L:\\Studia\\ProgrammingProjects\\EyeTrackerNEW\\src\\main\\resources\\haarcascade_lefteye_2splits.xml");
        rightEyeClassifier = new CascadeClassifier("L:\\Studia\\ProgrammingProjects\\EyeTrackerNEW\\src\\main\\resources\\haarcascade_righteye_2splits.xml");
        this.absoluteFaceSize = 0;

    }

    /**
     * The action triggered by pushing the button on the GUI
     */
    @FXML
    protected void startCamera() {
        // set a fixed width for the frame
        originalFrame.setFitWidth(600);
        // preserve image ratio
        originalFrame.setPreserveRatio(true);

        if (!this.cameraActive) {

            // start the video capture
            this.capture.open(0);

            // is the video stream available?
            if (this.capture.isOpened()) {
                this.cameraActive = true;

                // grab a frame every 33 ms (30 frames/sec)
                Runnable frameGrabber = new Runnable() {

                    public void run()
                    {
                        Image imageToShow = grabFrame();
                        originalFrame.setImage(imageToShow);
                    }
                };

                this.timer = Executors.newSingleThreadScheduledExecutor();
                this.timer.scheduleAtFixedRate(frameGrabber, initialDelay, framesPerSecond, TimeUnit.MILLISECONDS);

                // update the button content
                this.cameraButton.setText("Stop Camera");
            }
            else {
                // log the error
                System.err.println("Failed to open the camera connection...");
            }
        }
        else {
            // the camera is not active at this point
            this.cameraActive = false;
            // update again the button content
            this.cameraButton.setText("Start Camera");

            // stop the timer
            try {
                this.timer.shutdown();
                this.timer.awaitTermination(framesPerSecond, TimeUnit.MILLISECONDS);
            }
            catch (InterruptedException e) {
                // log the exception
                System.err.println("Exception in stopping the frame capture, trying to release the camera now... " + e);
            }

            // release the camera
            this.capture.release();
            // clean the frame
            this.originalFrame.setImage(null);
        }
    }

   /*Get frame from camera*/
    private Image grabFrame() {
        // init everything
        Image imageToShow = null;
        frame = new Mat();

        // check if the capture is open
        if (this.capture.isOpened()) {
            try {
                // read the current frame
                this.capture.read(frame);

                // if the frame is not empty, process it
                if (!frame.empty()) {
                    // face detection
                    this.detectAndDisplay(frame);

                    // convert the Mat object (OpenCV) to Image (JavaFX)
                    imageToShow = mat2Image(frame);
                }

            }
            catch (Exception e) {
                // log the (full) error
                System.err.println("ERROR: " + e);
            }
        }

        return imageToShow;
    }

    /*Detect face, eyes*/
    private void detectAndDisplay(Mat frame) {
        MatOfRect faces = new MatOfRect();
        grayFrame = new Mat();

        // convert the frame in gray scale
        Imgproc.cvtColor(frame, grayFrame, Imgproc.COLOR_BGR2GRAY);
        // equalize the frame histogram to improve the result
        Imgproc.equalizeHist(grayFrame, grayFrame);

        // compute minimum face size (20% of the frame height, in our case)
        if (this.absoluteFaceSize == 0)
        {
            int height = grayFrame.rows();
            if (Math.round(height * 0.2f) > 0)
            {
                this.absoluteFaceSize = Math.round(height * 0.2f);
            }
        }

        // detect faces
        this.faceCascade.detectMultiScale(grayFrame, faces, 1.1, 2, Objdetect.CASCADE_SCALE_IMAGE,
                new Size(this.absoluteFaceSize, this.absoluteFaceSize), new Size());


        Rect[] facesArray = faces.toArray();
        for (Rect faceRect : facesArray) {
            // draw rectangle around each face
            Imgproc.rectangle(frame, faceRect.tl(), faceRect.br(), new Scalar(0, 255, 0), 3);

            // compute the eye area
            eyearea = new Rect(faceRect.x +faceRect.width/8,(int)(faceRect.y + (faceRect.height/4.5)),faceRect.width - 2*faceRect.width/8,(int)(faceRect.height/3.0));

            // split it
            Rect eyearea_right = new Rect(faceRect.x +faceRect.width/16,(int)(faceRect.y + (faceRect.height/4.5)),(faceRect.width - 2*faceRect.width/16)/2,(int)( faceRect.height/3.0));
            Rect eyearea_left = new Rect(faceRect.x +faceRect.width/16 +(faceRect.width - 2*faceRect.width/16)/2,(int)(faceRect.y + (faceRect.height/4.5)),(faceRect.width - 2*faceRect.width/16)/2,(int)( faceRect.height/3.0));

            //draw splitted eye area
            Imgproc.rectangle(frame,eyearea_left.tl(),eyearea_left.br() , new Scalar(255,0, 0, 255), 2);
            Imgproc.rectangle(frame,eyearea_right.tl(),eyearea_right.br() , new Scalar(255, 0, 0, 255), 2);

            if(learn_frames<5){
                rightEyeTemplate = getTemplate(rightEyeClassifier,eyearea_right,24);
                leftEyeTemplate = getTemplate(lefEyeClassifier,eyearea_left,24);
                learn_frames++;
            }else{
                // Learning finished, use the new templates for template matching
                match_value = match_eye(eyearea_right,rightEyeTemplate,TM_SQDIFF);
                match_value = match_eye(eyearea_left,leftEyeTemplate,TM_SQDIFF);
            }
        }

    }


    private double  match_eye(Rect area, Mat mTemplate,int type){
        Point matchLoc;
        Mat mROI = grayFrame.submat(area);
        int result_cols =  mROI.cols() - mTemplate.cols() + 1;
        int result_rows = mROI.rows() - mTemplate.rows() + 1;
        //Check for bad template size
        if(mTemplate.cols()==0 ||mTemplate.rows()==0){
            return 0.0;
        }
        mResult = new Mat(result_cols, result_rows, CvType.CV_8U);

        switch (type){
            case TM_SQDIFF:
                Imgproc.matchTemplate(mROI, mTemplate, mResult, TM_SQDIFF) ;
                break;
            case TM_SQDIFF_NORMED:
                Imgproc.matchTemplate(mROI, mTemplate, mResult, TM_SQDIFF_NORMED) ;
                break;
            case TM_CCOEFF:
                Imgproc.matchTemplate(mROI, mTemplate, mResult, Imgproc.TM_CCOEFF) ;
                break;
            case TM_CCOEFF_NORMED:
                Imgproc.matchTemplate(mROI, mTemplate, mResult, Imgproc.TM_CCOEFF_NORMED) ;
                break;
            case TM_CCORR:
                Imgproc.matchTemplate(mROI, mTemplate, mResult, TM_CCORR) ;
                break;
            case TM_CCORR_NORMED:
                Imgproc.matchTemplate(mROI, mTemplate, mResult, TM_CCORR_NORMED) ;
                break;
        }

        Core.MinMaxLocResult mmres =  Core.minMaxLoc(mResult);
        // there is difference in matching methods - best match is max/min value
        if(type == TM_SQDIFF || type == TM_SQDIFF_NORMED)
        { matchLoc = mmres.minLoc; }
        else
        { matchLoc = mmres.maxLoc; }

        Point  matchLoc_tx = new Point(matchLoc.x+area.x,matchLoc.y+area.y);
        Point  matchLoc_ty = new Point(matchLoc.x + mTemplate.cols() + area.x , matchLoc.y + mTemplate.rows()+area.y );

        Imgproc.rectangle(frame, matchLoc_tx,matchLoc_ty, new Scalar(255, 255, 0, 255));

        if(type == TM_SQDIFF || type == TM_SQDIFF_NORMED)
        { return mmres.maxVal; }
        else
        { return mmres.minVal; }

    }

    private Mat getTemplate(CascadeClassifier classifier, Rect area, int size){
        Mat template = new Mat();
        Mat mROI = grayFrame.submat(area);
        MatOfRect eyes = new MatOfRect();
        Point iris = new Point();
        Rect eye_template = new Rect();
        classifier.detectMultiScale(mROI, eyes, 1.15, 2,Objdetect.CASCADE_FIND_BIGGEST_OBJECT|Objdetect.CASCADE_SCALE_IMAGE, new Size(30,30),new Size());

        Rect[] eyesArray = eyes.toArray();
        for (int i = 0; i < eyesArray.length; i++){
            Rect e = eyesArray[i];
            e.x = area.x + e.x;
            e.y = area.y + e.y;
            Rect eye_only_rectangle = new Rect((int)e.tl().x,(int)( e.tl().y + e.height*0.4),(int)e.width,(int)(e.height*0.6));
            // reduce ROI
            mROI = grayFrame.submat(eye_only_rectangle);
            Mat vyrez = frame.submat(eye_only_rectangle);
            // find the darkness point
            Core.MinMaxLocResult mmG = Core.minMaxLoc(mROI);
            // draw point to visualise pupil
            Imgproc.circle(vyrez, mmG.minLoc,2, new Scalar(255, 255, 255, 255),2);
            iris.x = mmG.minLoc.x + eye_only_rectangle.x;
            iris.y = mmG.minLoc.y + eye_only_rectangle.y;
            eye_template = new Rect((int)iris.x-size/2,(int)iris.y-size/2 ,size,size);
            Imgproc.rectangle(frame,eye_template.tl(),eye_template.br(),new Scalar(255, 0, 0, 255), 2);
            // copy area to template
            template = (grayFrame.submat(eye_template)).clone();
            return template;
        }
        return template;
    }

    //Convert a Mat object (OpenCV) in the corresponding image(to show)
    private Image mat2Image(Mat frame) {
        // create a temporary buffer
        MatOfByte buffer = new MatOfByte();
        // encode the frame in the buffer, according to the PNG format
        Imgcodecs.imencode(".png", frame, buffer);
        // build and return an Image created from the image encoded in the
        // buffer
        return new Image(new ByteArrayInputStream(buffer.toArray()));
    }

}