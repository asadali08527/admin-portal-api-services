package co.yabx.admin.portal.app.kyc.service.impl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import co.yabx.admin.portal.app.dto.dtoHelper.PagesDTOHeper;
import co.yabx.admin.portal.app.enums.KycStatus;
import co.yabx.admin.portal.app.enums.PageType;
import co.yabx.admin.portal.app.enums.ProductName;
import co.yabx.admin.portal.app.enums.Relationship;
import co.yabx.admin.portal.app.enums.UserType;
import co.yabx.admin.portal.app.kyc.dto.PagesDTO;
import co.yabx.admin.portal.app.kyc.dto.ProductDocumentsDTO;
import co.yabx.admin.portal.app.kyc.dto.UserDisclaimerDocumentsDTO;
import co.yabx.admin.portal.app.kyc.entities.AccountStatuses;
import co.yabx.admin.portal.app.kyc.entities.AddressDetails;
import co.yabx.admin.portal.app.kyc.entities.AttachmentDetails;
import co.yabx.admin.portal.app.kyc.entities.Attachments;
import co.yabx.admin.portal.app.kyc.entities.BankAccountDetails;
import co.yabx.admin.portal.app.kyc.entities.BusinessDetails;
import co.yabx.admin.portal.app.kyc.entities.FieldRemarks;
import co.yabx.admin.portal.app.kyc.entities.IntroducerDetails;
import co.yabx.admin.portal.app.kyc.entities.LiabilitiesDetails;
import co.yabx.admin.portal.app.kyc.entities.LoanPurposeDetails;
import co.yabx.admin.portal.app.kyc.entities.MonthlyTransactionProfiles;
import co.yabx.admin.portal.app.kyc.entities.Pages;
import co.yabx.admin.portal.app.kyc.entities.ProductDocuments;
import co.yabx.admin.portal.app.kyc.entities.User;
import co.yabx.admin.portal.app.kyc.entities.UserRelationships;
import co.yabx.admin.portal.app.kyc.entities.WorkEducationDetails;
import co.yabx.admin.portal.app.kyc.repositories.AccountStatusesRepository;
import co.yabx.admin.portal.app.kyc.repositories.AddressDetailsRepository;
import co.yabx.admin.portal.app.kyc.repositories.BankAccountDetailsRepository;
import co.yabx.admin.portal.app.kyc.repositories.FieldRemarksRepository;
import co.yabx.admin.portal.app.kyc.repositories.ProductDocumentsRepository;
import co.yabx.admin.portal.app.kyc.repositories.UserRelationshipsRepository;
import co.yabx.admin.portal.app.kyc.repositories.UserRepository;
import co.yabx.admin.portal.app.kyc.service.AndroidPushNotificationsService;
import co.yabx.admin.portal.app.kyc.service.AppConfigService;
import co.yabx.admin.portal.app.kyc.service.KYCService;
import co.yabx.admin.portal.app.kyc.service.UserService;

@Service
public class KYCServiceImpl implements KYCService {

	@Autowired
	private AccountStatusesRepository accountStatusesRepository;

	@Autowired
	private UserRelationshipsRepository userRelationshipsRepository;

	@Autowired
	private UserService userService;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private AddressDetailsRepository addressDetailsRepository;

	@Autowired
	private BankAccountDetailsRepository accountDetailsRepository;

	@Autowired
	private co.yabx.admin.portal.app.kyc.repositories.PagesRepository kycPagesRepository;

	@Autowired
	private FieldRemarksRepository fieldRemarksRepository;

	@Autowired
	private ProductDocumentsRepository productDocumentsRepository;

	@Autowired
	private AndroidPushNotificationsService androidPushNotificationsService;

	@Autowired
	private AppConfigService appConfigService;

	private static final Logger LOGGER = LoggerFactory.getLogger(KYCServiceImpl.class);

	public List<PagesDTO> findAllRetailers() {
		List<UserRelationships> dsrRetailersRelationships = userRelationshipsRepository
				.findByRelationship(Relationship.RETAILER);
		List<PagesDTO> pagesDTOs = new ArrayList<PagesDTO>();
		for (UserRelationships userRelationships : dsrRetailersRelationships) {
			pagesDTOs.addAll(userService.getUserDetails(userRelationships.getRelative(), PageType.RETAILERS));
		}
		return pagesDTOs;
	}

	@Override
	public List<PagesDTO> fetchRetailersByKycStatus(KycStatus kycStatus) {
		List<AccountStatuses> accountStatuses = accountStatusesRepository.findByKycVerified(kycStatus);
		List<PagesDTO> appPagesDTOList = new ArrayList<PagesDTO>();
		for (AccountStatuses accountStatus : accountStatuses) {
			User user = userRepository.findBymsisdnAndUserType(accountStatus.getMsisdn(), UserType.RETAILERS.name());
			if (user != null) {
				List<PagesDTO> appPagesDTO = new ArrayList<PagesDTO>();
				List<FieldRemarks> fieldRemarksList = fieldRemarksRepository.findByUserId(user.getId());
				Set<AddressDetails> addressDetailsSet = user.getAddressDetails();
				Set<BankAccountDetails> bankAccountDetailsSet = user.getBankAccountDetails();
				/*
				 * Set<BusinessDetails> businessDetailsSet = user.getBusinessDetails();
				 * Set<AttachmentDetails> attachmentDetailsSet = user.getAttachmentDetails();
				 * Set<LiabilitiesDetails> nominees = user.getLiabilitiesDetails();
				 * Set<IntroducerDetails> introducerDetails = user.getIntroducerDetails();
				 * Set<WorkEducationDetails> workEducationDetailsSet =
				 * user.getWorkEducationDetails(); Set<MonthlyTransactionProfiles>
				 * monthlyTransactionProfilesSet = user.getMonthlyTransactionProfiles();
				 * LoanPurposeDetails loanPurposeDetails = user.getLoanPurposeDetails();
				 */
				Set<BankAccountDetails> nomineeBankAccountDetailsSet = null;
				Set<BankAccountDetails> businessBankAccountDetailsSet = new HashSet<BankAccountDetails>();
				Set<AddressDetails> nomineeAddressDetailsSet = null;
				Set<AddressDetails> businessAddressDetailsSet = new HashSet<AddressDetails>();
				List<UserRelationships> userRelationships = userRelationshipsRepository
						.findByMsisdnAndRelationship(user.getMsisdn(), Relationship.NOMINEE);
				User nominee = userRelationships != null && !userRelationships.isEmpty()
						? userRelationships.get(0).getRelative()
						: null;
				nomineeAddressDetailsSet = nominee != null ? nominee.getAddressDetails() : null;
				nomineeBankAccountDetailsSet = nominee != null ? accountDetailsRepository.findByUser(user) : null;

				if (user.getBusinessDetails() != null) {
					user.getBusinessDetails().forEach(f -> {
						businessAddressDetailsSet.addAll(f.getAddressDetails());
					});
					user.getBusinessDetails().forEach(f -> {
						businessBankAccountDetailsSet.addAll(f.getBankAccountDetails());
					});
				}

				List<Pages> appPages = kycPagesRepository.findByPageType(PageType.RETAILERS);
				if (appPages == null)
					return null;
				for (Pages pages : appPages) {
					appPagesDTO.add(PagesDTOHeper.prepareAppPagesDto(pages, user, nominee, addressDetailsSet,
							nomineeAddressDetailsSet, businessAddressDetailsSet, bankAccountDetailsSet,
							nomineeBankAccountDetailsSet, businessBankAccountDetailsSet, PageType.RETAILERS.name(),
							fieldRemarksList));

				}
				appPagesDTOList.addAll(appPagesDTO);
			}
		}
		return appPagesDTOList;

	}

	@Override
	public AccountStatuses updateKycStatus(String msisdn, String username, KycStatus status) {
		AccountStatuses accountStatuses = accountStatusesRepository.findByMsisdn(msisdn);
		if (accountStatuses != null) {
			accountStatuses.setKycVerified(status);
			accountStatuses.setUpdatedBy(username);
			accountStatuses = accountStatusesRepository.save(accountStatuses);
			if (appConfigService.getBooleanProperty("FCM_NOTIFICATION_ENABLED", false))
				androidPushNotificationsService.notifyDSR(msisdn, username, status);
			return accountStatuses;
		}
		return null;
	}

	@Override
	public UserDisclaimerDocumentsDTO getDisclaimerDocuments(String msisdn) {
		User user = userRepository.findBymsisdnAndUserType(msisdn, UserType.RETAILERS.toString());
		if (user != null) {
			return getDisclaimerDocuments(user);
		}
		return null;
	}

	@Override
	public UserDisclaimerDocumentsDTO getDisclaimerDocuments(User user) {
		if (user != null) {
			UserDisclaimerDocumentsDTO disclaimerDocumentsDTO = new UserDisclaimerDocumentsDTO();
			disclaimerDocumentsDTO.setUserId(user.getId());
			disclaimerDocumentsDTO.setMsisdn(user.getMsisdn());
			List<ProductDocumentsDTO> productDocumentsDTOs = getUserDisclaimerDocuments(user, disclaimerDocumentsDTO);
			disclaimerDocumentsDTO.setDisclaimerDocuments(productDocumentsDTOs);
			return disclaimerDocumentsDTO;
		}
		return null;
	}

	private List<ProductDocumentsDTO> getUserDisclaimerDocuments(User user,
			UserDisclaimerDocumentsDTO disclaimerDocumentsDTO) {
		Set<AttachmentDetails> attachmentDetailsSet = user.getAttachmentDetails();
		List<ProductDocuments> productDocuments = productDocumentsRepository.findByProductName(ProductName.KYC);
		List<ProductDocumentsDTO> productDocumentsDTOs = new ArrayList<ProductDocumentsDTO>();
		for (ProductDocuments documents : productDocuments) {
			ProductDocumentsDTO productDocumentsDTO = new ProductDocumentsDTO();
			productDocumentsDTO.setDisplayOrder(documents.getDisplayOrder());
			productDocumentsDTO.setDocumentFor(documents.getDocumentFor());
			productDocumentsDTO.setDocumentName(documents.getDocumentName());
			productDocumentsDTO.setAttachmentType(
					documents.getAttachmentType() != null ? documents.getAttachmentType().toString() : null);
			productDocumentsDTO.setDocumentType(documents.getDocumentType());
			setFileName(user, productDocumentsDTO, disclaimerDocumentsDTO, attachmentDetailsSet, documents);
			productDocumentsDTOs.add(productDocumentsDTO);
		}
		return productDocumentsDTOs;
	}

	private void setFileName(User user, ProductDocumentsDTO productDocumentsDTO,
			UserDisclaimerDocumentsDTO disclaimerDocumentsDTO, Set<AttachmentDetails> attachmentDetailsSet,
			ProductDocuments documents) {
		Optional<AttachmentDetails> optional = attachmentDetailsSet.stream()
				.filter(f -> f.getDocumentType().equalsIgnoreCase(documents.getDocumentType())).findFirst();
		if (optional.isPresent()) {
			AttachmentDetails attachmentDetails = optional.get();
			if (attachmentDetails != null) {
				Set<Attachments> attachments = attachmentDetails.getAttachments();
				Optional<Attachments> optionalAttachments = attachments.stream().findFirst();
				if (optionalAttachments.isPresent()) {
					productDocumentsDTO.setFileName(optionalAttachments.get().getDocumentUrl());
				} else {
					productDocumentsDTO.setFileName(documents.getFileName());
					disclaimerDocumentsDTO.setDisclaimerDocRecieved(false);
				}
			} else {
				productDocumentsDTO.setFileName(documents.getFileName());
				disclaimerDocumentsDTO.setDisclaimerDocRecieved(false);
			}
		} else {
			productDocumentsDTO.setFileName(documents.getFileName());
			disclaimerDocumentsDTO.setDisclaimerDocRecieved(false);
		}
	}

}
