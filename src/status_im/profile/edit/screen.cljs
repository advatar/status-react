(ns status-im.profile.edit.screen
  (:require-macros [status-im.utils.views :refer [defview]])
  (:require [cljs.spec.alpha :as s]
            [clojure.string :as str]
            [reagent.core :as r]
            [re-frame.core :refer [dispatch]]
            [status-im.profile.styles :as st]
            [status-im.components.text-input-with-label.view :refer [text-input-with-label]]
            [status-im.components.styles :refer [color-blue color-gray5]]
            [status-im.components.status-bar :refer [status-bar]]
            [status-im.components.toolbar-new.view :refer [toolbar]]
            [status-im.components.toolbar-new.actions :as act]
            [status-im.i18n :refer [label]]
            [status-im.profile.screen :refer [colorize-status-hashtags]]
            [status-im.components.sticky-button :refer [sticky-button]]
            [status-im.components.camera :as camera]
            [status-im.components.chat-icon.screen :refer [my-profile-icon]]
            [status-im.components.context-menu :refer [context-menu]]
            [status-im.profile.validations :as v]
            [status-im.components.react :refer [view
                                                scroll-view
                                                keyboard-avoiding-view
                                                text
                                                touchable-highlight
                                                text-input]]
            [status-im.utils.utils :as utils :refer [clean-text]]
            [status-im.utils.platform :refer [ios?]]))

(defn edit-my-profile-toolbartoolbar []
  [toolbar {:title   (label :t/edit-profile)
            :actions [{:image :blank}]}])

(defview profile-name-input []
  [new-profile-name [:get-in [:profile-edit :name]]]
  [view
   [text-input-with-label {:label          (label :t/name)
                           :default-value  new-profile-name
                           :on-change-text #(dispatch [:set-in [:profile-edit :name] %])}]])

(def profile-icon-options
  [{:text  (label :t/image-source-gallery)
    :value #(dispatch [:open-image-picker])}
   {:text  (label :t/image-source-make-photo)
    :value (fn []
             (dispatch [:request-permissions
                        [:camera :write-external-storage]
                        (fn []
                          (camera/request-access
                            #(if % (dispatch [:navigate-to :profile-photo-capture])
                                   (utils/show-popup (label :t/error)
                                                     (label :t/camera-access-error)))))]))}])

(defn edit-profile-bage [contact]
  [view st/edit-profile-bage
   [view st/edit-profile-icon-container
    [context-menu
     [my-profile-icon {:account contact
                       :edit?   true}]
     profile-icon-options
     st/context-menu-custom-styles]]
   [view st/edit-profile-name-container
    [profile-name-input]]])

(defn edit-profile-status [{:keys [status edit-status?]}]
  (let [input-ref (r/atom nil)]
    [view st/edit-profile-status
     [scroll-view
      (if edit-status?
        [text-input
         {:ref               #(reset! input-ref %)
          :auto-focus        edit-status?
          :multiline         true
          :max-length        140
          :placeholder       (label :t/status)
          :style             st/profile-status-input
          :on-change-text    #(dispatch [:set-in [:profile-edit :status] (clean-text %)])
          :on-blur           #(dispatch [:set-in [:profile-edit :edit-status?] false])
          :blur-on-submit    true
          :on-submit-editing #(.blur @input-ref)
          :default-value     status}]
        [touchable-highlight {:on-press #(dispatch [:set-in [:profile-edit :edit-status?] true])}
         [view
          (if (str/blank? status)
            [text {:style st/add-a-status}
             (label :t/status)]
            [text {:style st/profile-status-text}
             (colorize-status-hashtags status)])]])]]))

(defn status-prompt [{:keys [status]}]
  (when (or (nil? status) (str/blank? status))
    [view st/status-prompt
     [text {:style st/status-prompt-text}
      (colorize-status-hashtags (label :t/status-prompt))]]))

(defview edit-my-profile []
  [current-account [:get-current-account]
   changed-account [:get :profile-edit]]
  {:component-will-unmount #(dispatch [:set-in [:profile-edit :edit-status?] false])}
  (let [profile-edit-data-valid? (s/valid? ::v/profile changed-account)
        profile-edit-data-changed? (or (not= (:name current-account) (:name changed-account))
                                       (not= (:status current-account) (:status changed-account))
                                       (not= (:photo-path current-account) (:photo-path changed-account)))]
    [keyboard-avoiding-view {:style st/profile}
     [status-bar]
     [edit-my-profile-toolbartoolbar]
     [view st/edit-my-profile-form
       [edit-profile-bage changed-account]
       [edit-profile-status changed-account]
       [status-prompt changed-account]]
     (when (and profile-edit-data-changed? profile-edit-data-valid?)
       [sticky-button (label :t/save) #(do
                                          (dispatch [:check-status-change (:status changed-account)])
                                          (dispatch [:account-update changed-account]))])]))
