// Author: Richard Bradford

package com.modelcoding.opensource.jsoncache.client

import com.modelcoding.opensource.jsoncache.CacheImageSender
import org.junit.Rule
import org.junit.rules.ExternalResource
import org.reactivestreams.Publisher
import org.reactivestreams.Subscriber;
import spock.lang.Specification

import static TestSuite.*

class CacheChangeSetProcessorSpecification extends Specification {

    @Rule
    private ExternalResource setup = perTestMethodSetup
    
    def "CacheChangeSetProcessor is created as expected"() {
        
        when:
        def input = Mock(Publisher)
        c.getCacheChangeSetProcessor(input)
        
        then:
        notThrown(Throwable)
        0 * input._
    }   
    
    def "CacheChangeSetProcessor cannot be created from bad parameters"() {
        
        when:
        c.getCacheChangeSetProcessor(null)
        
        then:
        thrown(IllegalArgumentException)
    }
    
    def "CacheChangeSetProcessor cannot be connected with bad parameters"() {
        
        when:
        def input = Mock(Publisher)
        def processor = c.getCacheChangeSetProcessor(input)
        processor.connect(null)
        
        then:
        thrown(IllegalArgumentException)
    }
    
    def "CacheChangeSetProcessor cannot be subscribed to with bad parameters"() {
        
        when:
        def input = Mock(Publisher)
        def processor = c.getCacheChangeSetProcessor(input)
        processor.connect(Mock(CacheImageSender))
        processor.subscribe(null)
        
        then:
        thrown(IllegalArgumentException)
    }
    
    def "CacheChangeSetProcessor cannot be requested to send cache image with bad parameters"() {
        
        when:
        def input = Mock(Publisher)
        def processor = c.getCacheChangeSetProcessor(input)
        processor.connect(Mock(CacheImageSender))
        processor.subscribe(Mock(Subscriber))
        processor.sendImageToSubscriber(null)
        
        then:
        thrown(IllegalArgumentException)
    }
    
    def "CacheChangeSetProcessor cannot be subscribed to unless connected"() {
        
        when:
        def input = Mock(Publisher)
        def processor = c.getCacheChangeSetProcessor(input)
        processor.subscribe(Mock(Subscriber))
        
        then:
        thrown(IllegalStateException)
    }
}
