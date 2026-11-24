/**
 * Copyright 2018 Google Inc. All Rights Reserved.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// If the loader is already loaded, just stop.
if (!self.define) {
  let registry = {};

  // Used for `eval` and `importScripts` where we can't get script URL by other means.
  // In both cases, it's safe to use a global var because those functions are synchronous.
  let nextDefineUri;

  const singleRequire = (uri, parentUri) => {
    uri = new URL(uri + ".js", parentUri).href;
    return registry[uri] || (
      
        new Promise(resolve => {
          if ("document" in self) {
            const script = document.createElement("script");
            script.src = uri;
            script.onload = resolve;
            document.head.appendChild(script);
          } else {
            nextDefineUri = uri;
            importScripts(uri);
            resolve();
          }
        })
      
      .then(() => {
        let promise = registry[uri];
        if (!promise) {
          throw new Error(`Module ${uri} didn’t register its module`);
        }
        return promise;
      })
    );
  };

  self.define = (depsNames, factory) => {
    const uri = nextDefineUri || ("document" in self ? document.currentScript.src : "") || location.href;
    if (registry[uri]) {
      // Module is already loading or loaded.
      return;
    }
    let exports = {};
    const require = depUri => singleRequire(depUri, uri);
    const specialDeps = {
      module: { uri },
      exports,
      require
    };
    registry[uri] = Promise.all(depsNames.map(
      depName => specialDeps[depName] || require(depName)
    )).then(deps => {
      factory(...deps);
      return exports;
    });
  };
}
define(['./workbox-afac4cd2'], (function (workbox) { 'use strict';

  self.skipWaiting();
  workbox.clientsClaim();
  /**
   * The precacheAndRoute() method efficiently caches and responds to
   * requests for URLs in the manifest.
   * See https://goo.gl/S9QRab
   */
  workbox.precacheAndRoute([{
    "url": "xi_deck.html",
    "revision": "912a22a385b3014e4b7d476d33fb63bf"
  }, {
    "url": "registerSW.js",
    "revision": "1872c500de691dce40960bb85481de07"
  }, {
    "url": "pwa-maskable-512x512.png",
    "revision": "a8dad1ef293f6b917e3d945adab70bc1"
  }, {
    "url": "pwa-512x512.png",
    "revision": "e9d8a979f630a83dcbfb78bf9ae6fc98"
  }, {
    "url": "pwa-192x192.png",
    "revision": "809ad9efdfc44df89e56a64f824ff0ca"
  }, {
    "url": "index.html",
    "revision": "4e473d353a93ef561c83695b10f50132"
  }, {
    "url": "favicon.png",
    "revision": "a90f4b9c47023be7e6e2dbe3ad0f9bed"
  }, {
    "url": "apple-touch-icon.png",
    "revision": "10762125b21e17f72f1a82e7ccafec92"
  }, {
    "url": "assets/index-P2Xu9kJm.js",
    "revision": null
  }, {
    "url": "assets/aistudio/index__5__ce6013d6.html",
    "revision": null
  }, {
    "url": "apple-touch-icon.png",
    "revision": "10762125b21e17f72f1a82e7ccafec92"
  }, {
    "url": "favicon.png",
    "revision": "a90f4b9c47023be7e6e2dbe3ad0f9bed"
  }, {
    "url": "pwa-192x192.png",
    "revision": "809ad9efdfc44df89e56a64f824ff0ca"
  }, {
    "url": "pwa-512x512.png",
    "revision": "e9d8a979f630a83dcbfb78bf9ae6fc98"
  }, {
    "url": "pwa-maskable-512x512.png",
    "revision": "a8dad1ef293f6b917e3d945adab70bc1"
  }, {
    "url": "manifest.webmanifest",
    "revision": "03412ddc804f753bff12c059853622c6"
  }], {});
  workbox.cleanupOutdatedCaches();
  workbox.registerRoute(new workbox.NavigationRoute(workbox.createHandlerBoundToURL("index.html")));
  workbox.registerRoute(/^https:\/\/fonts\.googleapis\.com\/.*/i, new workbox.CacheFirst({
    "cacheName": "google-fonts-cache",
    plugins: [new workbox.ExpirationPlugin({
      maxEntries: 10,
      maxAgeSeconds: 31536000
    }), new workbox.CacheableResponsePlugin({
      statuses: [0, 200]
    })]
  }), 'GET');
  workbox.registerRoute(/^https:\/\/fonts\.gstatic\.com\/.*/i, new workbox.CacheFirst({
    "cacheName": "gstatic-fonts-cache",
    plugins: [new workbox.ExpirationPlugin({
      maxEntries: 10,
      maxAgeSeconds: 31536000
    }), new workbox.CacheableResponsePlugin({
      statuses: [0, 200]
    })]
  }), 'GET');

}));
