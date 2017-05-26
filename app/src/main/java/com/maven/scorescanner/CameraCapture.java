package com.maven.scorescanner;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.SurfaceView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by nathan on 5/25/2017.
 */

public class CameraCapture extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    private static final String TAG = "Capture Image";
    private CameraBridgeViewBase mOpenCvCameraView;
    private boolean              mIsJavaCamera = true;
    private MenuItem mItemSwitchCamera = null;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.camera_capture);


        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.tutorial1_activity_java_surface_view);

        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);

        mOpenCvCameraView.setCvCameraViewListener(this);
    }

    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    public void onCameraViewStarted(int width, int height) {
    }

    public void onCameraViewStopped() {
    }

    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
//        return inputFrame.rgba();
        return getPoints(inputFrame);

    }

    public Mat getPoints(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        Mat mRgba = new Mat();
        mRgba = inputFrame.rgba();

        Mat grey = new Mat();
        // Make it greyscale
        Imgproc.cvtColor(mRgba, grey, Imgproc.COLOR_RGBA2GRAY);

        // init contours arraylist
        List<MatOfPoint> contours = new ArrayList<MatOfPoint>(200);

        // because findContours modifies the image I back it up
        Mat greyCopy = new Mat();
        grey.copyTo(greyCopy);

        //blur
        Imgproc.GaussianBlur(greyCopy, greyCopy, new Size(5,5), 2);
        Imgproc.adaptiveThreshold(grey, grey, 255, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY_INV, 15, 4);


        Imgproc.findContours(greyCopy, contours, new Mat(), Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_NONE);
        // Now I have my controus pefectly


        MatOfPoint2f mMOP2f1 = new MatOfPoint2f();

        //a list for only selected contours
        List<MatOfPoint> SelectedContours = new ArrayList<MatOfPoint>(400);

        double h_point = 0;
        double l_point = 0;

        for(int i=0;i<contours.size();i++)
        {

            MatOfPoint m = contours.get(i);
            List<Point> list = m.toList();

            for(int j = 0; j<list.size(); j++) {
                Point p = list.get(j);

                if(p.y > h_point){
                    h_point = p.y;
                    l_point = p.x;
                }
            }
            SelectedContours.add(contours.get(i));
        }

//        Mat lines = new Mat();
//
//        Imgproc.HoughLinesP(grey, lines, 1 , Math.PI/180, 80, 400, 10);
//
//        for (int x = 0; x < lines.rows(); x++)
//        {
//            double[] vec = lines.get(x,0);
//            double x1 = vec[0],
//                    y1 = vec[1],
//                    x2 = vec[2],
//                    y2 = vec[3];
//            Point start = new Point(x1, y1);
//            Point end = new Point(x2, y2);
//            double dx = x1 - x2;
//            double dy = y1 - y2;
//
//            System.out.println(TAG+" Point Coordinates: x1=>"+x1+" y1=>"+y1+" x2=>"+x2+" y2=>"+y2);
//            double dist = Math.sqrt (dx*dx + dy*dy);
//
//            if(dist>300.d)  // show those lines that have length greater than 300
//                Imgproc.line(mRgba, start, end, new Scalar(255,0, 0, 255),2);// here initimg is the original image.
//
//        }
        Imgproc.drawContours(mRgba, SelectedContours, -1, new Scalar(0,255,0,255), 1);

        return mRgba;
    }
}
