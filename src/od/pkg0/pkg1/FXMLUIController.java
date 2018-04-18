/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package od.pkg0.pkg1;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.objdetect.Objdetect;
import org.opencv.videoio.VideoCapture;

public class FXMLUIController implements Initializable {

    @FXML
    private Button start_btn;

    @FXML
    private ImageView currentFrame;

    private ScheduledExecutorService tim;
    private VideoCapture capt = new VideoCapture();
    private boolean camActive = false;
    private static int camId = 0;
    
    private CascadeClassifier faceCascade;
    private int absFaceSize;

    @FXML
    protected void startCamera(ActionEvent e) {

        if (this.camActive == false) {
            this.capt.open(camId);
            this.camActive = true;
            Runnable frameGrabber = new Runnable() {
                @Override
                public void run() {
                    Mat frame = grabFrame();
                    Image imgToShow = Utils.mat2Image(frame);
                    updateImg(currentFrame, imgToShow);
                    
                }
            };

            this.tim = Executors.newSingleThreadScheduledExecutor();
            this.tim.scheduleAtFixedRate(frameGrabber, 0, 33, TimeUnit.MILLISECONDS);

            this.start_btn.setText("Stop");
        } else {
            this.stopAcquisition();
            this.start_btn.setText("Start");
            this.camActive = false;
        }

    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        this.capt = new VideoCapture();
        this.faceCascade = new CascadeClassifier();
        this.absFaceSize = 0;
        this.faceCascade.load("resources/haarcascades/haarcascade_frontalface_alt.xml"); 
        //this.faceCascade.load("resources/lbpcascades/lbpcascade_frontalface.xml");
    }

    private Mat grabFrame() {
        Mat frame = new Mat();
        try {
            this.capt.read(frame);
            if(!frame.empty())
                this.detectAndDisplay(frame);
             

        } catch (Exception e) {
            System.err.println("obraz");
        }
        return frame;
    }

    private void updateImg(ImageView view, Image img) {
        Utils.onFXThread(view.imageProperty(), img);
    }

    private void stopAcquisition() {
        if (this.tim != null && !this.tim.isShutdown()) {
            try {

                this.tim.shutdown();
                this.tim.awaitTermination(33, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {

                System.err.println(e);
            }
        }

        if (this.capt.isOpened()) {

            this.capt.release();
        }
    }
    
    private void detectAndDisplay(Mat frame)
    {
        MatOfRect faces = new MatOfRect();
        Mat grayFrame = new Mat();
        
        Imgproc.cvtColor(frame, grayFrame, Imgproc.COLOR_BGR2GRAY);
        Imgproc.equalizeHist(grayFrame, grayFrame);
        
        if(this.absFaceSize == 0)
        {
            int wys = grayFrame.rows();
            if(Math.round(wys * 0.2f)>0)
            {
                this.absFaceSize = Math.round(wys * 0.2f);
            }
        }
        
        this.faceCascade.detectMultiScale(grayFrame, faces, 1.1, 2, Objdetect.CASCADE_SCALE_IMAGE, new Size(this.absFaceSize, this.absFaceSize), new Size());
        
        Rect[] facesA = faces.toArray();
        for(int i=0; i<facesA.length; i++)
            Imgproc.rectangle(frame, facesA[i].tl(), facesA[i].br(), new Scalar(0, 255, 0), 3);        
    }

}
