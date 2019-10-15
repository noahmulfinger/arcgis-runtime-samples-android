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

package com.esri.arcgisruntime.sample.exploresceneinflyoverar;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.esri.arcgisruntime.geometry.Envelope;
import com.esri.arcgisruntime.layers.Layer;
import com.esri.arcgisruntime.loadable.LoadStatus;
import com.esri.arcgisruntime.mapping.ArcGISScene;
import com.esri.arcgisruntime.mapping.ArcGISTiledElevationSource;
import com.esri.arcgisruntime.mapping.MobileScenePackage;
import com.esri.arcgisruntime.mapping.NavigationConstraint;
import com.esri.arcgisruntime.mapping.view.Camera;
import com.esri.arcgisruntime.toolkit.ar.ArcGISArView;

public class MainActivity extends AppCompatActivity {

  private static final String TAG = MainActivity.class.getSimpleName();

  private ArcGISArView mArView;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    requestPermissions();
  }

  private void displaySceneInAr() {
    mArView = findViewById(R.id.arView);
    mArView.registerLifecycle(getLifecycle());
    // disable touch interactions with the scene view
    mArView.getSceneView().setOnTouchListener((view, motionEvent) -> true);

    // load the mobile scene package with a path to the .mspk file
    MobileScenePackage sanDiegoScenePackage = new MobileScenePackage(
        Environment.getExternalStorageDirectory() + "/ArcGIS/Samples/ScenePackage/sandiego-pc.mspk");
    sanDiegoScenePackage.loadAsync();
    sanDiegoScenePackage.addDoneLoadingListener(() -> {
      if (sanDiegoScenePackage.getLoadStatus() == LoadStatus.LOADED) {

        // get the first scene
        ArcGISScene scene = sanDiegoScenePackage.getScenes().get(0);

        // create an elevation source and add it to the scene
        ArcGISTiledElevationSource elevationSource = new ArcGISTiledElevationSource(
            getString(R.string.world_terrain_service_url));
        scene.getBaseSurface().getElevationSources().add(elevationSource);
        scene.getBaseSurface().setNavigationConstraint(NavigationConstraint.STAY_ABOVE);

        // add the scene to the scene view
        mArView.getSceneView().setScene(scene);

        // get the first layer and load it
        Layer sanDiegoLayer = scene.getOperationalLayers().get(0);
        sanDiegoLayer.loadAsync();
        sanDiegoLayer.addDoneLoadingListener(() -> {
          if (sanDiegoLayer.getLoadStatus() == LoadStatus.LOADED) {
            // get the extent of the layer
            Envelope envelope = sanDiegoLayer.getFullExtent();
            // use its center to set the origin camera
            Camera camera = new Camera(envelope.getCenter().getY(), envelope.getCenter().getX(), 250, 0, 90, 0);
            mArView.setOriginCamera(camera);
          } else {
            String error = "Error loading layer: " + sanDiegoLayer.getLoadError().getMessage();
            Toast.makeText(this, error, Toast.LENGTH_LONG).show();
            Log.e(TAG, error);
          }
        });

        // set the translation factor to enable rapid movement through the scene
        mArView.setTranslationFactor(1000);
      } else {
        String error = "Error loading mobile map package: " + sanDiegoScenePackage.getLoadError().getMessage();
        Toast.makeText(this, error, Toast.LENGTH_LONG).show();
        Log.e(TAG, error);
      }
    });
  }

  /**
   * Request read external storage for API level 23+.
   */
  private void requestPermissions() {
    // define permission to request
    String[] reqPermission = { Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE };
    int requestCode = 2;
    if (ContextCompat.checkSelfPermission(this, reqPermission[0]) == PackageManager.PERMISSION_GRANTED
        && ContextCompat.checkSelfPermission(this, reqPermission[1]) == PackageManager.PERMISSION_GRANTED) {
      displaySceneInAr();
    } else {
      // request permission
      ActivityCompat.requestPermissions(this, reqPermission, requestCode);
    }
  }

  /**
   * Handle the permissions request response.
   */
  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
      displaySceneInAr();
    } else {
      // report to user that permission was denied
      Toast.makeText(this, getString(R.string.permission_required_for_ar), Toast.LENGTH_SHORT).show();
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
