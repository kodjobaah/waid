require.config({
    paths: {
        "jquery": "assets/converse.js/components/jquery/jquery",
        "locales": "assets/converse.js/locale/locales",
        "jquery.tinysort": "assets/converse.js/components/tinysort/src/jquery.tinysort",
        "underscore": "assets/converse.js/components/underscore/underscore",
        "backbone": "assets/converse.js/components/backbone/backbone",
        "backbone.localStorage": "assets/converse.js/components/backbone.localStorage/backbone.localStorage",
        "strophe": "assets/converse.js/components/strophe/strophe",
        "strophe.muc": "assets/converse.js/components/strophe.muc/index",
        "strophe.roster": "assets/converse.js/components/strophe.roster/index",
        "strophe.vcard": "assets/converse.js/components/strophe.vcard/index",
        "strophe.disco": "assets/converse.js/components/strophe.disco/index",
        "salsa20": "assets/converse.js/components/otr/build/dep/salsa20",
        "bigint": "assets/converse.js/src/bigint",
        "crypto.core": "assets/converse.js/components/otr/vendor/cryptojs/core",
        "crypto.enc-base64": "assets/converse.js/components/otr/vendor/cryptojs/enc-base64",
        "crypto.md5": "assets/converse.js/components/crypto-js-evanvosberg/src/md5",
        "crypto.evpkdf": "assets/converse.js/components/crypto-js-evanvosberg/src/evpkdf",
        "crypto.cipher-core": "assets/converse.js/components/otr/vendor/cryptojs/cipher-core",
        "crypto.aes": "assets/converse.js/components/otr/vendor/cryptojs/aes",
        "crypto.sha1": "assets/converse.js/components/otr/vendor/cryptojs/sha1",
        "crypto.sha256": "assets/converse.js/components/otr/vendor/cryptojs/sha256",
        "crypto.hmac": "assets/converse.js/components/otr/vendor/cryptojs/hmac",
        "crypto.pad-nopadding": "assets/converse.js/components/otr/vendor/cryptojs/pad-nopadding",
        "crypto.mode-ctr": "assets/converse.js/components/otr/vendor/cryptojs/mode-ctr",
        "crypto": "assets/converse.js/src/crypto",
        "eventemitter": "assets/converse.js/components/otr/build/dep/eventemitter",
        "otr": "assets/converse.js/components/otr/build/otr",
        "converse-dependencies": "assets/converse.js/src/deps-full"
    },

    // define module dependencies for modules not using define
    shim: {
        'backbone': {
            //These script dependencies should be loaded before loading
            //backbone.js
            deps: [
                'underscore',
                'jquery'
                ],
            //Once loaded, use the global 'Backbone' as the
            //module value.
            exports: 'Backbone'
        },
        'underscore':           { exports: '_' },
        'crypto.aes':           { deps: ['crypto.cipher-core'] },
        'crypto.cipher-core':   { deps: ['crypto.enc-base64', 'crypto.evpkdf'] },
        'crypto.enc-base64':    { deps: ['crypto.core'] },
        'crypto.evpkdf':        { deps: ['crypto.md5'] },
        'crypto.hmac':          { deps: ['crypto.core'] },
        'crypto.md5':           { deps: ['crypto.core'] },
        'crypto.mode-ctr':      { deps: ['crypto.cipher-core'] },
        'crypto.pad-nopadding': { deps: ['crypto.cipher-core'] },
        'crypto.sha1':          { deps: ['crypto.core'] },
        'crypto.sha256':        { deps: ['crypto.core'] },
        'jquery.tinysort':      { deps: ['jquery'] },
        'strophe':              { deps: ['jquery'] },
        'strophe.disco':        { deps: ['strophe'] },
        'strophe.muc':          { deps: ['strophe', 'jquery'] },
        'strophe.roster':       { deps: ['strophe'] },
        'strophe.vcard':        { deps: ['strophe'] }
    }
});

require(["jquery", "converse"], function(require, $, converse) {
    window.converse = converse;
});
