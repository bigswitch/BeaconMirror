/**
 * Copyright (c) 2009, Nathan Bubna
 * Dual licensed under the MIT and GPL licenses:
 *   http://www.opensource.org/licenses/mit-license.php
 *   http://www.gnu.org/licenses/gpl.html
 *
 * This plugin depends upon the Values plugin to provide an enhanced clone method.
 * This "xclone" method will store the element it is called on as a template
 * of sorts, and then create sibling clones that have the specified values
 * automatically set and are also automatically inserted below (or, optionally,
 * above) where the original prototype was located.  In essence, this enhances
 * the template-ish abilities of the Values plugin in a fashion similar to what
 * the jsRepeater plugin does, but with only standard XML/HTML syntax.
 * 
 * This is particulary useful for AJAX applications, allowing the initial document
 * to contain just an "empty" element set that serves as a location marker and template
 * for clones which are filled with model data loaded/generated on the client side.
 *
 * @name xclone
 * @version 0.3.1
 * @cat Plugins/XClone
 * @author Nathan Bubna
 */
(function ($) {
    var X = $.xclone = {
        version: "0.3.1",
        defaults: {
            count: 1,
            // clone events by default
            events: true,
            insert: true,
            insertAtTop: false
        },

        // prototype retrieval/caching
        getProto: function(options) {
            var $context = $(this.context),
                selector = this.selector;
            //log('using selector \'', selector, '\' for ', this);
            // using find() can leave us with a disembodied context in
            // some circumstances. so try to provide a proper context here.
            if (this.context != document && $context.parents().size() == 0) {
                //log('changing context from', $context);
                if (this.prevObject && this.prevObject.length > 0 && this.prevObject[0] != this.context) {
                    $context = $(this.prevObject[0]);
                } else {
                    $context = $(document);
                }
            }
            // don't distinguish complex selectors from their last segment
            if (selector.indexOf(' ') >= 0) {
                //log('changing selector from', selector);
                selector = selector.substring(selector.indexOf(' ')+1);
            }
            var key = 'clone$'+selector+'$proto',
                $proto = $context.data(key);
            if ($proto === undefined) {
                //log('creating proto', key, 'in', $context);
                $proto = X.toProto.call(this, options);
                $proto.context = $context[0];
                $proto.selector = selector; 
                $context.data(key, $proto);
            }
            // else log('found', $proto, 'for', key, 'in', $context);
            return $proto;
        },

        // prototype creation
        toProto: function(options) {
            // copy proto from first selected element only
            var $original = $(this[0]),
                // look for location markers
                $prev = $original.prev(),
                $next = $original.next(),
                $proto = $original.clone(options.events);

            // set location markers
            if ($prev.length > 0) {
                $proto.$prev = $prev;
            }
            if ($next.length > 0) {
                $proto.$next = $next;
            }
            $proto.$parent = $original.parent();
            $proto.count = 0;

            // destroy the original
            $original.remove();
            return $proto;
        },

        // clones proto, sets values (if any), performs insert (if wanted),
        // calls the callback (if any), and returns the new clone
        copy: function(values, options) {
            var $clone = this.clone(options.events);
            values.xcloneTotal = ++this.count;
            X.fill.call($clone, values, options);
            values.xcloneTotal = undefined;
            if (options.insert) {
                X.insert.call(this, $clone, options);
            }
            if (options.callback) {
                callback.call($clone, values, options);
            }
            return $clone;
        },

        // fills the prototype with the specified values; you can
        // override this to use something besides the Values plugin
        fill: function(values, options) {
            if ($.values.isOptions(options)) {
                this.values(values, options);
            } else {
                this.values(values);
            }
        },

        insert: function($clone, options) {
            //log('insert', this, $clone, options);
            if (options.insertAtTop) {
                if (check(this, '$prev')) {
                    this.$prev.after($clone);
                } else {
                    this.$parent.prepend($clone);
                }
            } else if (check(this, '$next')) {
                this.$next.before($clone);
            } else {
                this.$parent.append($clone);
            }

            // ensure saved sibling exists and hasn't left parent
            function check($p, a) {
                var ok = $p[a] && $p[a][0].parentNode == $p.$parent[0];
                if (!ok) {
                    $p[a] = undefined;
                }
                return ok;
            }
        }
    };

    $.fn.xclone = function(values, b, c) {
        var options = $.extend({}, X.defaults, c);
        if ($.isFunction(b)) {
            options.callback = b;
        } else {
            $.extend(options, b);
        }

        var $proto = X.getProto.call(this, options),
            count = options.count;
        // no args => 0 blank copies
        if (values === undefined) {
            count = 0;
            values = {};
        // arrays of values means multiple clones
        } else if ($.isArray(values)) {
            count = values.length;
            if (count == 1) {
                values = values[0];
            }
        // a number means multiple "empty" clones
        } else if (typeof values == "number") {
            count = values;
            values = {};
        // a boolean means one "empty" clone with that as the events and insert setting
        } else if (typeof values == "boolean") {
            options.events = options.insert = values;
            values = {};
        }
        //log('count:',count); log('values:',values); log('options:',options);

        if (count > 1) {
            var $all, $c, i=0, arr=$.isArray(values), vals;
            for (; i < count; i++) {
                vals = arr ? values[i] : values;
                vals.xcloneInSet = i;
                $c = X.copy.call($proto, vals, options);
                vals.xcloneInSet = undefined;
                if ($all === undefined) {
                    $all = $c;
                } else {
                    $all.add($c);
                }
            }
            return $all;
        }

        if (count == 1) {
            return X.copy.call($proto, values, options);
        }

        // count == 0, return $proto since original is destroyed
        return $proto;
    };

})(jQuery);