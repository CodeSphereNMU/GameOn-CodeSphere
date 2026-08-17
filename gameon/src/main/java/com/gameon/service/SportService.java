package com.gameon.service;

import com.gameon.exception.ResourceNotFoundException;
import com.gameon.model.entity.FormatPosition;
import com.gameon.model.entity.Sport;
import com.gameon.model.entity.SportFormat;
import com.gameon.repository.FormatPositionRepository;
import com.gameon.repository.SportFormatRepository;
import com.gameon.repository.SportRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service handling sport and format lookup operations.
 * Sports, formats, and positions are reference data (read-only in normal usage).
 */
@Service
public class SportService {

    private final SportRepository sportRepository;
    private final SportFormatRepository sportFormatRepository;
    private final FormatPositionRepository formatPositionRepository;

    public SportService(SportRepository sportRepository,
                        SportFormatRepository sportFormatRepository,
                        FormatPositionRepository formatPositionRepository) {
        this.sportRepository = sportRepository;
        this.sportFormatRepository = sportFormatRepository;
        this.formatPositionRepository = formatPositionRepository;
    }

    @Transactional(readOnly = true)
    public List<Sport> getAllSports() {
        return sportRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Sport getSportById(Long sportId) {
        return sportRepository.findById(sportId)
                .orElseThrow(() -> new ResourceNotFoundException("Sport", sportId));
    }

    @Transactional(readOnly = true)
    public List<SportFormat> getFormatsForSport(Long sportId) {
        return sportFormatRepository.findBySportSportId(sportId);
    }

    @Transactional(readOnly = true)
    public SportFormat getFormatById(Long formatId) {
        return sportFormatRepository.findById(formatId)
                .orElseThrow(() -> new ResourceNotFoundException("Sport Format", formatId));
    }

    @Transactional(readOnly = true)
    public SportFormat getFormatWithPositions(Long formatId) {
        return sportFormatRepository.findByIdWithPositions(formatId)
                .orElseThrow(() -> new ResourceNotFoundException("Sport Format", formatId));
    }

    @Transactional(readOnly = true)
    public List<FormatPosition> getPositionsForFormat(Long formatId) {
        return formatPositionRepository.findByFormatIdWithPositions(formatId);
    }

    /**
     * Gets all sport formats the user can create listings for
     * (based on sports on their profile).
     */
    @Transactional(readOnly = true)
    public List<SportFormat> getFormatsForUserSports(Long userId) {
        return sportFormatRepository.findFormatsForUserSports(userId);
    }

    /**
     * Gets formats for a specific set of sport IDs.
     */
    @Transactional(readOnly = true)
    public List<SportFormat> getFormatsBySportIds(List<Long> sportIds) {
        return sportFormatRepository.findBySportIds(sportIds);
    }

    @Transactional(readOnly = true)
    public Set<Long> getPositionIdsForFormat(Long formatId) {
        return getPositionsForFormat(formatId).stream()
                .map(fp -> fp.getPosition().getPositionId())
                .collect(Collectors.toSet());
    }

    @Transactional(readOnly = true)
    public Map<Long, String> getPositionNamesForFormat(Long formatId) {
        return getPositionsForFormat(formatId).stream()
                .collect(Collectors.toMap(
                        fp -> fp.getPosition().getPositionId(),
                        fp -> fp.getPosition().getPositionName()));
    }
}
