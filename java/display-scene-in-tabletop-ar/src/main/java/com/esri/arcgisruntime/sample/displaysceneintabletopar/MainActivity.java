/*
 *  Copyright 2019 Esri
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.esri.arcgisruntime.sample.displaysceneintabletopar;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.Toast;

import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.layers.IntegratedMeshLayer;
import com.esri.arcgisruntime.mapping.ArcGISScene;
import com.esri.arcgisruntime.mapping.ArcGISTiledElevationSource;
import com.esri.arcgisruntime.mapping.NavigationConstraint;
import com.esri.arcgisruntime.mapping.Surface;
import com.esri.arcgisruntime.mapping.view.Camera;
import com.esri.arcgisruntime.mapping.view.DefaultSceneViewOnTouchListener;
import com.esri.arcgisruntime.toolkit.ar.ArcGISArView;

public class MainActivity extends AppCompatActivity {

  private static final String TAG = MainActivity.class.getSimpleName();

  private boolean mHasConfiguredScene = false;

  private ArcGISArView mArView;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    requestPermissions();
  }

  /**
   * Setup the Ar View to use ArCore and tracking. Also add a touch listener to the scene view which checks for single
   * taps on a plane, as identified by ArCore. On tap, set the initial transformation matrix and load the scene.
   */
  private void setupArView() {

    mArView = findViewById(R.id.arView);
    mArView.registerLifecycle(getLifecycle());

    ArcGISTiledElevationSource elevationSource = new ArcGISTiledElevationSource(
        "https://elevation3d.arcgis.com/arcgis/rest/services/WorldElevation3D/Terrain3D/ImageServer");
    Surface surface = new Surface();
    surface.getElevationSources().add(elevationSource);

    IntegratedMeshLayer integratedMeshLayer = new IntegratedMeshLayer(
        "https://tiles.arcgis.com/tiles/FQD0rKU8X5sAQfh8/arcgis/rest/services/VRICON_Yosemite_Sample_Integrated_Mesh_scene_layer/SceneServer");



    ArcGISScene scene = new ArcGISScene();
    scene.getOperationalLayers().add(integratedMeshLayer);

    scene.setBaseSurface(surface);

    // on tap
    mArView.getSceneView().setOnTouchListener(new DefaultSceneViewOnTouchListener(mArView.getSceneView()) {
      @Override public boolean onSingleTapConfirmed(MotionEvent motionEvent) {
        // get the tapped point as a graphics point
        android.graphics.Point screenPoint = new android.graphics.Point(Math.round(motionEvent.getX()),
            Math.round(motionEvent.getY()));
        // if initial transformation set correctly
        if ( mArView.setInitialTransformationMatrix(screenPoint)) {
          mArView.getSceneView().setScene(scene);
          // when the scene has loaded, set navigation constraint and opacity to see below the surface
          scene.addDoneLoadingListener(() -> {
            // ensure the navigation constraint is set to NONE
            scene.getBaseSurface().setNavigationConstraint(NavigationConstraint.NONE);
            // set opacity to view content beneath the base surface
            scene.getBaseSurface().setOpacity(0.5f);
            // set translation factor and origin camera for scene placement in AR
            // set the translation factor based on scene content width and desired physical size
            mArView.setTranslationFactor(500);
            // find the center point of the scene content
            Point centerPoint = integratedMeshLayer.getFullExtent().getCenter();
            // find the altitude of the surface at the center
            ListenableFuture<Double> elevationFuture = mArView.getSceneView().getScene().getBaseSurface()
                .getElevationAsync(centerPoint);
            elevationFuture.addDoneListener(() -> {
              try {
                double elevation = elevationFuture.get();
                // create a new origin camera looking at the bottom center of the scene
                mArView.setOriginCamera(
                    new Camera(new Point(centerPoint.getX(), centerPoint.getY(), elevation), 1000, 0, 90, 0));
              } catch (Exception e) {
                Log.e(TAG, "Error getting elevation at point: " + e.getMessage());
              }
            });
          });
        } else {
          Toast.makeText(MainActivity.this, "ARCore did not detect the tapped point as a plane", Toast.LENGTH_LONG).show();
        }
        return super.onSingleTapConfirmed(motionEvent);
      }
    });
  }

  /**
   * Request read external storage for API level 23+.
   */
  private void requestPermissions() {
    // define permission to request
    String[] reqPermission = { Manifest.permission.CAMERA };
    int requestCode = 2;
    if (ContextCompat.checkSelfPermission(this, reqPermission[0]) == PackageManager.PERMISSION_GRANTED) {
      setupArView();
    } else {
      // request permission
      ActivityCompat.requestPermissions(this, reqPermission, requestCode);
    }
  }

  /**
   * Handle the permissions request response.
   */
  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
      @NonNull int[] grantResults) {
    if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
      setupArView();
    } else {
      // report to user that permission was denied
      Toast.makeText(this, getString(R.string.tabletop_map_permission_denied), Toast.LENGTH_SHORT).show();
    }
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
  }

  @Override
  protected void onPause() {
    if (mArView != null) {
      mArView.stopTracking();
    }
    super.onPause();
  }

  @Override
  protected void onResume() {
    super.onResume();
    if (mArView != null) {
      mArView.startTracking(ArcGISArView.ARLocationTrackingMode.IGNORE);
    }
  }
}
