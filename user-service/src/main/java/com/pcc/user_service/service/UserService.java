package com.pcc.user_service.service;

import com.pcc.user_service.model.UserProfile;
import com.pcc.user_service.repository.UserProfileRepository;
//import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
//@RequiredArgsConstructor
public class UserService {

    public UserService(UserProfileRepository profileRepository) {
        this.profileRepository = profileRepository;
    }

    private final UserProfileRepository profileRepository;

    public UserProfile getProfile(Long userId) {
        return profileRepository.findById(userId)
                .orElse(null);
    }

    public UserProfile updateProfile(Long userId, UserProfile newProfile) {
        Optional<UserProfile> existing = profileRepository.findById(userId);

        UserProfile profile;
        if (existing.isPresent()) {
            profile = existing.get();
        } else {
            profile = new UserProfile();
            profile.setUserId(userId);
        }
        profile.setFullName(newProfile.getFullName());
        profile.setBio(newProfile.getBio());
        profile.setBirthDate(newProfile.getBirthDate());
        profile.setLocation(newProfile.getLocation());
        profile.setProfilePictureUrl(newProfile.getProfilePictureUrl());

        return profileRepository.save(profile);
    }
}