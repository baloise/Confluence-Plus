import com.atlassian.confluence.event.events.content.blogpost.BlogPostEvent
import com.atlassian.confluence.labels.LabelManager

import static com.atlassian.sal.api.component.ComponentLocator.getComponent

BlogPostEvent bpEvent = event as BlogPostEvent
if(bpEvent.blogPost.title.contains('Liste der relevanten Changes')){
    LabelManager labelMan = getComponent(LabelManager.class)
	labelMan.addLabel(bpEvent.blogPost,labelMan.getLabel('nogit'))
}