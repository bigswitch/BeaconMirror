/**
 * Copyright (c) 2009, Nathan Bubna
 * Dual licensed under the MIT and GPL licenses:
 *   http://www.opensource.org/licenses/mit-license.php
 *   http://www.gnu.org/licenses/gpl.html
 *
 * To retrieve values, simply do:
 *
 *      var values = $('.demo').values();
 *
 * This will look at the element with class 'demo' and its descendants
 * to find all elements with a 'name' attribute and then smartly
 * grabs the "value" for each of those elements.  By "smartly", i mean that
 * it use $el.val() for input elements, $el.find('option:selected').text()
 * for select elements, $el.attr('value') for elements with value attributes
 * and so on. For unrecognized elements, $el.text() and $el.html() are the
 * usual fallbacks; in other words, it tries to grab the
 * displayed values by default, if that makes sense. It also trims whitespace
 * when it uses text(). Once each value is retrieved it is added to a hash/JSON
 * object (that's object, not string) using the value of the 'name' attribute as
 * its key, and in the end, that object is returned, giving you easy access to
 * the values.
 *
 * If you pass in an object to this method, like:
 *
 *      $('.demo').values({ foo: 'bar', answer: 42 });
 *
 * it will reverse the process and set those values to the elements
 * with matching names.
 * 
 * If you pass in a different HTML element(s), like:
 *
 *      $('.demo').values($('form#foo'));
 *
 * then it will grab the values from <form id="foo">'s children
 * and copy them to the descendants of the element(s) with class 'demo'.
 *
 * If you wish to set or get a single named value, this plugin does accept
 * string keys to identify the single value you wish to get/set.  Just do
 * something like:
 *
 *      var foo = $('.demo').values('foo')
 *
 * to get the value with the name 'foo' from the element with the class 'demo'
 * or its descendant elements.  To set a value, just do:
 *
 *      $('.demo').values('bar', 42);
 *
 * and any of the elements whose name attribute has the value 'bar' will have
 * their value set to 42.
 *
 * As of 1.3, this plugin also handles heirarchical or "nested" values structures
 * both when setting and getting values.  Assuming you had markup like this:
 *
 *      <div id="a" name="a">
 *        <span name="b">b</span>
 *        <p name="c">
 *          <span name="d">d</span>
 *        </p>
 *      </div>
 *
 * Then doing $('#a').values({onlyNest: true}) should return this:
 *
 *      {
 *          a: {
 *              b: "b",
 *              c: {
 *                  d: "d"
 *              }
 *          }
 *      }
 *
 * Values "knows" to do this nesting when a "generic" node like a DIV or P has a
 * name attribute and has child nodes that also have named attributes.  Of course,
 * if you don't set the "onlyNest" option to true, then you will get all the values
 * in both nested and flat forms:
 *
 *      {
 *          a: {
 *              b: "b",
 *              c: {
 *                  d: "d"
 *              }
 *          },
 *          b: "b",
 *          c: {
 *              d: "d"
 *          },
 *          d: "d"
 *      }
 *
 * This is done for both backwards and general compatibility.  If you want only
 * a flat set of values returned, then you can set the "onlyFlat" option to true
 * and just get the text (or html, if no text) values of the child nodes:
 *
 *      {
 *          a: "b d",
 *          b: "b",
 *          c: "d",
 *          d: "d"
 *      }
 *
 * And of course, it should be said that you can set values with/to these structures
 * just as easily.
 *
 * Also new in 1.3 is the ability to handle arrays of values objects when setting data.
 * This is something of an unpolished feature, but allows for more complex JSON 
 * structures to be set.  In the event of more values objects being in the array than
 * there are elements in the jQuery selection being operated on, then copies will
 * be cloned and inserted in order to accept the extra values.  To understand, think
 * of table rows.  This is ability is similar to that of the sister XClone plugin.
 * Currently, the getting of values does not quite mirror this properly in all situations.
 * Like i said, it's a bit of an unpolished addition right now. :)
 *
 * When setting values, you have the option (as of 1.3.5) to pass in functions for
 * the values.  These will be called with the matching element as "this" and the current
 * workspace and options as arguments.  The value returned by the function (if any) is
 * then set to the element.  This allows for a variety of advanced behaviors.
 *
 * All of the example method calls above will accept an options object as a
 * last argument.  Or, you can just override the defaults at: V.defaults
 * The available options are:
 *    keyAttr: to change the attribute containing the value key (default is 'name')
 *    onlyNest: to remove inner values from the outer returned object so there are
 *              no duplicates (default is false, only applies to getting all values)
 *    onlyFlat: to never return heirarchical, "nested" values and to flatten any
 *              value objects being set via toString() before setting (default is false)
 *    nodeFilter: to specify a selector used to filter out certain descendant elements (default is undefined)
 *    copyToData: to also set values in the data() for the container element (default is true)
 *    copyToAttr: to also set values as attributes of the container element (default is false)
 *    setEvents: to fire a custom 'set.values' event for each field value set (default is false)
 *    setAllEvent: to fire a custom 'setAll.values' event when setAll runs (default is false)
 *    setter: custom function to handle setting of values or name of a provided setter (default is undefined)
 *    getter: custom function to handle retrieval of values or name of a provided getter (default is undefined)
 *    noClone: to prevent creation of clones when an array of values objects is passed in
 *             and there aren't enough selected elements to match all the values objects
 *    insertAtTop: to add new elements before existing ones (instead of after) when cloning (default is false)
 *    useSelectValue: to set whether the returned value for select elements is
 *                    the value of the selected option instead of its text (default is false)
 *    uncheckedValue: to set the value to return when a checkbox or radio button is
 *                    unchecked.  if undefined, the element is skipped when unchecked.
 *                    if true, the value is always returned, checked or unchecked. any
 *                    other setting for this value is returned itself (default is undefined).
 * 
 * The copyToData and copyToAttr settings serve as a way to ensure that any
 * set data is not entirely "lost" should there be no element with a matching 'name'
 * attribute for one or more of the keys.  I generally use them as more of a
 * debugging utility than anything else.
 *
 * NOTE: if multiple elements with the same name/key are found during a "get"
 * call and those values are not equal, then they are pushed into an array
 * which is associated with that key.  This also works in reverse;
 * if multiple values for the same name/key are found during a "set" call,
 * and there are multiple matching elements, then the values are applied
 * to those elements in order.  If there are fewer values than elements,
 * the values are looped.  If fewer elements than values, the extra values
 * are ignored.
 *
 * Also, this plugin is extremely configurable and extensible.
 * Just tweak override the various methods and settings in the $.values
 * object to change or extend the behaviors.
 *
 * @version 1.4
 * @name values
 * @cat Plugins/Values
 * @author Nathan Bubna
 */
(function ($) {
    // $.values() == $(document.body).values()
    var V = $.values = function(a, b, c) {
        return $(document.body).values(a, b, c);
    };
    // expose functions and defaults for extension/configuration
    $.extend(V, {
        version: "1.4",
        defaults : {
            // keep in guessed usage order for speed
            copyToData: false,
            useSelectValue: false,
            setter: undefined,
            getter: undefined,
            onlyNest: false,
            nodeFilter: undefined,
            setEvents: false,
            setAllEvent: false,
            onlyFlat: false,
            noClone: false,
            insertAtTop: false,
            uncheckedValue: undefined,
            keyAttr: 'name',
            copyToAttr: false,
            includeCounts: false
        },
        isOptions: function(o) {
            if (o !== null && typeof o == "object")
                for (var i in V.defaults)
                    if (o[i] !== undefined)
                        return true;
            return false;
        },
        setAll: function(values, opts) {
            if ($.isArray(values) && values.length > 0) {
                // recurse down array structures
                return V.setArrayToAll.call(this, values, opts);
            }
            
            // base case
            for (var i in values) {
                V.setOne.call(this, i, values[i], opts);
            }
            if (opts.copyToData) this.data('values', values);
            if (opts.setAllEvent) this.trigger('setAll.values', values, opts);
            return this;
        },
        setArrayToAll: function(values, opts) {
            var m = values.length, n = this.size(), last;
            if (n > 0 && m > 0) {
                last = V.setAll.call($(this[0]), values[0], opts);
                if (m > 1) {
                    var i, tgt;
                    for (i=1; i<m && (i<n || !opts.noClone); i++) {
                        tgt = i < n ? $(this[i]) : last.clone(true);
                        V.setAll.call(tgt, values[i], opts);
                        if (i >= n) {
                            last[opts.insertAtTop ? 'before' : 'after'](tgt);
                            this.add(tgt);
                            last = tgt;
                        }
                    }
                }
            }
            return this;
        },
        setOne: function(key, value, opts) {
            // may be multiple fields w/same name
            var select = '['+opts.keyAttr+'='+key+']',
                $fields = this.find(select);
            if (this.is(select)) $fields = $fields.add(this);
            if (opts.nodeFilter) $fields = $fields.filter(opts.nodeFilter);
            if (opts.copyToData) this.data(key, value);
            if (opts.copyToAttr) this.attr(key, value);

            var work = V.createWorkspace($fields, key, value, opts);
            work.fields.each(function() {
                V.setValue.call($(this), work, opts);
            });
        },
        createWorkspace: function($fields, key, value, opts) {
            var work = { fields: $fields,
                         fieldCount: $fields.size(),
                         key: key,
                         index: 0 };
            if ($.isArray(value) && value.length > 0) {
                work.values = value;
                for (var i=0,m=value.length; i<m; i++) {
                    if (value[i] !== null) {
                        if (work.value !== undefined) {
                            if (work.fieldCount == 1) {
                                V.manyValuesOneField(work, value, $fields, opts);
                            }
                            break;
                        }
                        work.value = value[i];
                    }
                }
            } else {
                work.value = value;
                if (typeof value == "string" && value.indexOf(',') >= 0) {
                    work.split = true;
                    work.values = value.split(',');
                }
            }
            return work;
        },
        manyValuesOneField: function(work, values, $field, opts) {
            if (opts.noClone || $field.children().length === 0) {
                // if objects, just take the first
                work.value = typeof values[0] == "object" ? values[0] : values.toString();
            } else {
                work.value = values;
            }
        },
        setValue: function(work, opts) {
            // coerce value into proper form
            var v = work.value;
            if ($.isFunction(v)) work.fn = v;
            if (work.fn) v = work.fn.call(this, work, opts);
            if (opts.onlyFlat && typeof v == "object") v = v.toString();
            work.value = v === null ? '' : v;

            // find and call matching setter for this node/value
            var set = opts.setter || V.set[nameOf(this)];
            if (set) {
                ($.isFunction(set) ? set : V.set[set]).call(this, work, opts);
            } else {
                for (var i=0,list=V.setters,m=list.length; i<m; i++) {
                    if (V.set[list[i]].call(this, work, opts)) break;
                }
            }

            // fire event and increment (if needed)
            if (opts.setEvents) {
                this.trigger('set.values', [work.key, work.value, work, opts]);
            }
            if (work.fieldCount > 1) {
                work.index++;
                if (work.values && !work.split) work.value = work.values[work.index];
            }
        },
        setters: ['value','nested','html','attr','text'],
        set: {
            value: function(work, opts) {
                var k = 'value';
                if (k == opts.keyAttr) return false;
                if (this.attr(k) !== undefined) return this.attr(k, work.value);
            },
            nested: function(work, opts) {
                var v = work.value
                return typeof v === "object" ? this.values(v, opts) : false;
            },
            html: function(work) {
                var v = work.value, i;
                if (typeof v == "string" &&
                    (i = v.indexOf('<')) >= 0 && v.indexOf('>') > i) return this.html(v);
            },
            attr: function(work, opts) {
                var k = work.key;
                if (k == opts.keyAttr || k == 'id') return false;
                if (this.attr(k) !== undefined) return this.attr(k, work.value);
            },
            text: function(work) {
                return this.text(work.value);
            },
            fill: function(work) {
                var o = this.text(), n = o.replace('{'+work.key+'}', work.value);
                if (n != o) return this.text(n);
            },
            input: function(work, opts) {
                var node = this[0], type = node.type.toLowerCase();
                if (type == 'checkbox' || type == 'radio') {
                    node.checked = (node.value == work.value);
                    if (!node.checked && work.values) {
                        node.checked = false;
                        var has = node.value, vals = work.values;
                        for (var i=0,m=vals.length; i<m; i++) {
                            if (has == vals[i]) {
                                node.checked = true;
                                break;
                            }
                        }
                    }
                } else {
                    node.value = work.value;
                }
            },
            select: function(work, opts) {
                var node = this[0], options = node.options,
                    mult = (node.type != "select-one" && work.values !== undefined),
                    useTxt = !opts.useSelectValue;
                for (var i=0,m=options.length; i<m; i++) {
                    var option = options[i],
                        has = (useTxt ? $.trim(option.text) : $(option).val());
                    if (mult) {
                        option.selected = false;
                        for (var j=0,n=work.values.length; j<n; j++) {
                            if (has == work.values[j]) {
                                option.selected = true;
                                break;
                            }
                        }
                    } else {
                        option.selected = has == work.value;
                    }
                }
            },
            textarea: function(work, opts) {
                this[0].value = work.value;
            },
            form: function(work, opts) {
                if (typeof work.value == "object") {
                    this.values(work.value, opts);
                } else {
                    this.attr('action', work.value);
                }
            },
            iframe: function(work, opts) {
                this[0].url = work.value;
            },
            img: function(work, opts) {
                this.attr('src', work.value);
            },
            embed: function(work, opts) {
                this.attr('src', work.value);
            }
        },
        getAll: function(opts) {
            var selector = '['+opts.keyAttr+']',
                $fields = this.find(selector),
                vals = {}, counts = {};
            if (this.is(selector)) $fields = $fields.add(this);
            if (opts.nodeFilter)   $fields = $fields.filter(opts.nodeFilter);

            // gather the keys and drop dupes
            $fields.each(function() {
                var key = $(this).attr(opts.keyAttr);
                if (key && key != '' && !vals[key]) {
                    vals[key] = key;
                    counts[key] = 0;
                }
            });
            // get values for each key
            for (var key in vals) {
                var got = V.getOne.call(this, key, opts, $fields);
                vals[key] = got.val;
                counts[key] = got.count;
            }
            if (opts.includeCounts) vals.valuesCounts = counts;
            if (opts.onlyNest)      V.deepClean(vals, opts);
            return vals;
        },
        deepClean: function(vals, opts, parents) {
            var k, v, p, skip = opts.includeCounts ? 'valuesCounts' : null;
            for (k in vals) {
                if (k != skip && typeof (v = vals[k]) == "object") {
                    if (!p) p = parents ? parents.concat([vals]) : [vals];
                    V.deepClean(v, opts, p);
                }
                if (parents) {
                    for (var i=0,m=parents.length; i<m; i++) delete parents[i][k];
                }
            }
        },
        getOne: function(key, opts, $fields) {
            var selector = '['+opts.keyAttr+'='+key+']',
                results = [], // for all
                result, // for one
                same = true; // decider

            if ($fields === undefined) {
                $fields = this.find(selector);
                if (this.is(selector)) {
                    $fields = $fields.add(this);
                }
                if (opts.nodeFilter) {
                    $fields = $fields.filter(opts.nodeFilter);
                }
            } else {
                // assume nodeFilter was already applied
                $fields = $fields.filter(selector);
            }
            $fields.each(function() {
                var val = V.getValue.call($(this), key, opts);
                if (val !== undefined) {
                    if (result !== undefined && result != val) {
                        same = false;
                    }
                    result = val;
                    results.push(result);
                }
            });
            if (same) {
                if (result === undefined) {
                    result = null;
                }
                return { count: results.length, val: result };
            }
            return { count: results.length, val: results };
        },
        getValue: function(key, opts) {
            var get = opts.getter || V.get[nameOf(this)];
            if (!get) {
                for (var ret,list=V.getters,i=0,m=list.length; i<m; i++) {
                    ret = V.get[list[i]].call(this, key, opts);
                    if (ret !== undefined) return ret;
                }
            }
            if (!$.isFunction(get)) get = V.get[get];
            return get.call(this, key, opts);
        },
        getters: ['value','nested','html','attr','text'],
        get: {
            value: function(k, opts) {
                return V.get.attr.call(this, k, opts);
            },
            nested: function(k, opts) {
                var $kids = this.children(), v;
                if ($kids.size() > 0) {
                    v = $kids.values(opts);
                    if (hasProps(v)) return v;
                }
            },
            html: function() {
                return ($.trim(this.text()) == '') ? this.html() : undefined;
            },
            attr: function(k, opts) {
                var v = this.attr(k);
                return (v == '' || k == opts.keyAttr) ? undefined : v;
            },
            text: function() {
                return this.text();
            },
            input: function(k, opts) {
                var node = this[0];
                if (opts.uncheckedValue !== true &&
                    (node.type == 'checkbox' || node.type == 'radio') && !node.checked) {
                    return opts.uncheckedValue;
                }
                return node.value;
            },
            option: function() {
                var node = this[0];
                return (node.attributes.value || {}).specified ? node.value : node.text;
            },
            select: function(k, opts) {
                var $selected = this.find('option:selected');
                if ($selected.size() == 0) {
                    return null;
                }
                var one = $selected.size() == 1,
                    val = one ? null : [];
                $selected.each(function() {
                    var v = opts.useSelectValue ? this.value : $.trim($(this).text());
                    if (one) {
                        val = v;
                    } else {
                        val.push(v);
                    }
                });
                return val;
            },
            textarea: function(k, opts) {
                return this[0].value;
            },
            form: function(k, opts) {
                if (!opts.one) {
                    var vals = this.children().values(opts);
                    if (hasProps(vals)) {
                        return vals;
                    }
                }
                return this.attr('action');
            },
            iframe: function() {
                return this[0].url;
            },
            img: function() {
                return this.attr('src');
            },
            embed: function() {
                return this.attr('src');
            }
        }
    });

    $.fn.values = function(a, b, c) {
        var opts = $.extend({}, V.defaults);
        // grab any options in the args and then wipe that arg
        if (c) {
            opts = $.extend(opts, c);
        } else if (V.isOptions(b)) {
            opts = $.extend(opts, b);
            b = undefined;
        } else if (V.isOptions(a)) {
            opts = $.extend(opts, a);
            a = undefined;
        }

        // get all
        if (!a) return V.getAll.call(this, opts);

        // get/set one
        if (typeof a == "string") { // one value
            opts.one = true;
            if (b === undefined) {
                return V.getOne.call(this, a, opts).val;
            }
            V.setOne.call(this, a, b, opts);
            return this;
        }

        // set all
        if (a.nodeType) a = $(a);// element->selection
        if (a.jquery) a = V.getAll.call(a, opts);// selection->values
        V.setAll.call(this, a, opts);
        return this;
    };

    // returns true if there are properties in this object
    // of course, adding to Object.prototype busts this
    function hasProps(o) { for (i in o) return true; }
    // returns the node name of a jQuery selection
    function nameOf(n) { return n[0].nodeName.toLowerCase(); }


})(jQuery);