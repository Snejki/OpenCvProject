/*
TODO śledzenie obiektów 2 różnych kolorach
 */
package od.pkg0.pkg1;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Slider;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
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
    @FXML
    private ImageView maskImg;
    @FXML
    private ImageView morphImg;
    
    @FXML
    private Slider hueMin;
    @FXML
    private Slider hueMax;
    @FXML
    private Slider satMin;
    @FXML
    private Slider satMax;
    @FXML
    private Slider valMin;
    @FXML
    private Slider valMax;
    

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
            Runnable frameGrabber = () -> {
                Mat frame = grabFrame();
                Image imgToShow = Utils.mat2Image(frame);
                updateImg(currentFrame, imgToShow);
                //updateImg(maskImg, imgToShow);
                //updateImg(morphImg, imgToShow);
            };

            this.tim = Executors.newSingleThreadScheduledExecutor();
            this.tim.scheduleAtFixedRate(frameGrabber, 0, 33, TimeUnit.MILLISECONDS);

            this.start_btn.setText("Stop");
        } else {
            this.currentFrame.setImage(null);
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
        
    }

    private Mat grabFrame() {
        
        Mat frame = new Mat();
        Mat blurredImg = new Mat();
        Mat hsvImg = new Mat();
        Mat mask = new Mat();
        Mat morphOutput = new Mat();
        
        this.capt.read(frame);
        //wykrywanie twarzy
        /*try {
            if(!frame.empty())
                this.detectAndDisplay(frame);
             

        } catch (Exception e) {
            System.err.println("obraz" + e);
        }*/
        
        //usuwa szum
        Imgproc.blur(frame, blurredImg, new Size(7, 7));
        
        //zamienia na HSV
        Imgproc.cvtColor(blurredImg, hsvImg, Imgproc.COLOR_BGR2HSV);
        
        //obsługa sliderów
        Scalar minV = new Scalar(this.hueMin.getValue(), this.satMin.getValue(), this.valMin.getValue());
        Scalar maxV = new Scalar(this.hueMax.getValue(), this.satMax.getValue(), this.valMax.getValue());
        
        Core.inRange(hsvImg, minV, maxV, mask);
        this.updateImg(this.maskImg, Utils.mat2Image(mask));
        
        Mat dilateElement = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(24, 24));
        Mat erodeElement = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(12, 12));
        
        Imgproc.erode(mask, morphOutput, erodeElement);
        Imgproc.erode(mask, morphOutput, erodeElement);
        
        Imgproc.dilate(mask, morphOutput, dilateElement);
        Imgproc.dilate(mask, morphOutput, dilateElement);
        
        this.updateImg(this.morphImg, Utils.mat2Image(morphOutput));
        frame = this.detect(morphOutput, frame);
        
        return frame;
        
        
    }

    private void updateImg(ImageView view, Image img) {
        Utils.onFXThread(view.imageProperty(), img);
    }
    
    private Mat detect(Mat m, Mat frame) {
        List<MatOfPoint> ctr = new ArrayList<>();
        Mat hie = new Mat();
        Imgproc.findContours(m, ctr, hie, Imgproc.RETR_CCOMP, Imgproc.CHAIN_APPROX_SIMPLE);
        
        if (hie.size().height > 0 && hie.size().width > 0)
{
        for(int x = 0; x >= 0; x = (int) hie.get(0, x)[0])
            Imgproc.drawContours(frame, ctr, x, new Scalar(0, 255, 0));
}
        return frame;
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
            Imgproc.rectangle(frame, facesA[i].tl(), facesA[i].br(), new Scalar(255, 255, 255), 3);        
    }

}
