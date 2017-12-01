// Author: Richard Bradford

package com.modelcoding.opensource.jsoncache

import net.javacrumbs.jsonunit.core.Option
import org.junit.Rule
import org.junit.rules.ExternalResource
import spock.lang.Shared
import spock.lang.Specification

import static com.modelcoding.opensource.jsoncache.TestSuite.*
import static net.javacrumbs.jsonunit.JsonAssert.*

class CacheChangeSetSpecification extends Specification {

    @Rule
    private ExternalResource setup = perTestMethodSetup

    @Shared
        content =
            [
                aThing      : "stuff",
                anotherThing: 12
            ]
    @Shared
        someContent = asJsonNode(content)
    @Shared
        otherContent =
            [
                aThing      : "otherStuff",
                anotherThing: 12
            ]
    @Shared
        someOtherContent = asJsonNode(otherContent)
    @Shared
        puts = [
            m.getCacheObject("Id1", "Type", someContent),
            m.getCacheObject("Id2", "Type", someOtherContent)
        ] as Set
    @Shared
        removes = [
            m.getCacheRemove("Id3"),
            m.getCacheRemove("Id4")
        ] as Set


    def "CacheChangeSet is created as expected"() {

        when:
        def cacheChangeSet = m.getCacheChangeSet("id", puts, removes, true)

        then:
        cacheChangeSet.id == "id"
        cacheChangeSet.puts == puts
        cacheChangeSet.removes == removes
        cacheChangeSet.cacheImage
    }

    def "CacheChangeSet cannot be created from bad parameters"() {

        when:
        m.getCacheChangeSet(null, puts, removes, false)

        then:
        thrown(NullPointerException)

        when:
        m.getCacheChangeSet("id", null, removes, false)

        then:
        thrown(NullPointerException)

        when:
        m.getCacheChangeSet("id", puts, null, false)

        then:
        thrown(NullPointerException)
    }

    def "Equal CacheChangeSets are equal"() {

        expect:
        m.getCacheChangeSet("id", p, r, ci) == m.getCacheChangeSet("not id", p, r, ci)
        m.getCacheChangeSet("id", p, r, ci).hashCode() == m.getCacheChangeSet("not id", p, r, ci).hashCode()

        where:
        p    | r       | ci
        puts | removes | false
        puts | removes | true
    }

    def "Equality must not rely on a specific implementation"() {

        expect:
        m.getCacheChangeSet("id", puts, removes, false) == new CacheChangeSet() {

            @Override
            Set<? extends CacheObject> getPuts() {
                CacheChangeSetSpecification.this.puts
            }

            @Override
            Set<? extends CacheRemove> getRemoves() {
                CacheChangeSetSpecification.this.removes
            }

            @Override
            boolean isCacheImage() {
                false
            }

            @Override
            String getId() {
                "not id"
            }
        }
    }

    def "Unequal CacheChangeSets are not equal"() {

        expect:
        m.getCacheChangeSet("id", p, r, ci) != m.getCacheChangeSet("not id", p_, r_, ci_)

        where:
        p         | r       | ci    | p_   | r_        | ci_
        puts      | removes | false | puts | [] as Set | false
        [] as Set | removes | false | puts | removes   | false
        puts      | removes | false | puts | removes   | true
    }

    def "CacheChangeSet accessors do not expose CacheChangeSet to mutation"() {

        setup:
        def thePuts = new HashSet(puts)
        def theRemoves = new HashSet(removes)

        when:
        def cacheChangeSet = m.getCacheChangeSet("id", puts, removes, false)
        def gotPuts = cacheChangeSet.puts
        def gotRemoves = cacheChangeSet.removes
        try {
            gotPuts.iterator().remove()
        }
        catch(ignored) {
        }
        try {
            gotPuts << m.getCacheObject("Id5", "Type", someContent)
        }
        catch(ignored) {
        }
        try {
            gotRemoves.clear()
        }
        catch(ignored) {
        }
        thePuts << m.getCacheObject("Id5", "Type", someContent)
        theRemoves.clear()

        then:
        cacheChangeSet.puts == new HashSet(puts)
        cacheChangeSet.removes == new HashSet(removes)
        !cacheChangeSet.cacheImage
    }
}
