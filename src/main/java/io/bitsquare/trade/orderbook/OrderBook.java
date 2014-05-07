package io.bitsquare.trade.orderbook;

import com.google.inject.Inject;
import io.bitsquare.bank.BankAccount;
import io.bitsquare.gui.market.orderbook.OrderBookListItem;
import io.bitsquare.settings.Settings;
import io.bitsquare.trade.Direction;
import io.bitsquare.trade.Offer;
import io.bitsquare.user.Arbitrator;
import io.bitsquare.user.User;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;

public class OrderBook
{
    private static final Logger log = LoggerFactory.getLogger(OrderBook.class);

    private Settings settings;
    private User user;

    protected ObservableList<OrderBookListItem> allOffers = FXCollections.observableArrayList();
    private FilteredList<OrderBookListItem> filteredList = new FilteredList<>(allOffers);
    // FilteredList does not support sorting, so we need to wrap it to a SortedList
    private SortedList<OrderBookListItem> offerList = new SortedList<>(filteredList);


    ///////////////////////////////////////////////////////////////////////////////////////////
    // Constructor
    ///////////////////////////////////////////////////////////////////////////////////////////

    @Inject
    public OrderBook(Settings settings, User user)
    {
        this.settings = settings;
        this.user = user;
    }


    ///////////////////////////////////////////////////////////////////////////////////////////
    // Public API
    ///////////////////////////////////////////////////////////////////////////////////////////

    public void updateFilter(OrderBookFilter orderBookFilter)
    {
        filteredList.setPredicate(new Predicate<OrderBookListItem>()
        {
            @Override
            public boolean test(OrderBookListItem orderBookListItem)
            {
                Offer offer = orderBookListItem.getOffer();
                BankAccount currentBankAccount = user.getCurrentBankAccount();

                if (orderBookFilter == null
                        || offer == null
                        || currentBankAccount == null
                        || orderBookFilter.getDirection() == null)
                    return false;

                // The users current bank account currency must match the offer currency (1 to 1)
                boolean currencyResult = currentBankAccount.getCurrency().equals(offer.getCurrency());

                // The offer bank account country must match one of the accepted countries defined in the settings (1 to n)
                boolean countryResult = countryInList(offer.getBankAccountCountryLocale(), settings.getAcceptedCountryLocales());

                // One of the supported languages from the settings must match one of the offer languages (n to n)
                boolean languageResult = languagesInList(settings.getAcceptedLanguageLocales(), offer.getAcceptedLanguageLocales());

                // Apply updateFilter only if there is a valid value set
                // The requested amount must be lower or equal then the offer amount
                boolean amountResult = true;
                if (orderBookFilter.getAmount() > 0)
                    amountResult = orderBookFilter.getAmount() <= offer.getAmount().doubleValue();

                // The requested trade direction must be opposite of the offerList trade direction
                boolean directionResult = !orderBookFilter.getDirection().equals(offer.getDirection());

                // Apply updateFilter only if there is a valid value set
                boolean priceResult = true;
                if (orderBookFilter.getPrice() > 0)
                {
                    if (offer.getDirection() == Direction.SELL)
                        priceResult = orderBookFilter.getPrice() >= offer.getPrice();
                    else
                        priceResult = orderBookFilter.getPrice() <= offer.getPrice();
                }

                // The arbitrator defined in the offer must match one of the accepted arbitrators defined in the settings (1 to n)
                boolean arbitratorResult = arbitratorInList(offer.getArbitrator(), settings.getAcceptedArbitrators());


                boolean result = currencyResult
                        && countryResult
                        && languageResult
                        && amountResult
                        && directionResult
                        && priceResult
                        && arbitratorResult;

                       /*
                log.debug("result = " + result +
                        ", currencyResult = " + currencyResult +
                        ", countryResult = " + countryResult +
                        ", languageResult = " + languageResult +
                        ", amountResult = " + amountResult +
                        ", directionResult = " + directionResult +
                        ", priceResult = " + priceResult +
                        ", arbitratorResult = " + arbitratorResult
                );

                log.debug("currentBankAccount.getCurrency() = " + currentBankAccount.getCurrency() +
                        ", offer.getCurrency() = " + offer.getCurrency());
                log.debug("offer.getCountryLocale() = " + offer.getBankAccountCountryLocale() +
                        ", settings.getAcceptedCountryLocales() = " + settings.getAcceptedCountryLocales().toString());
                log.debug("settings.getAcceptedLanguageLocales() = " + settings.getAcceptedLanguageLocales() +
                        ", offer.getAcceptedLanguageLocales() = " + offer.getAcceptedLanguageLocales());
                log.debug("currentBankAccount.getBankAccountType().getType() = " + currentBankAccount.getBankAccountType().getType() +
                        ", offer.getBankAccountTypeEnum() = " + offer.getBankAccountTypeEnum());
                log.debug("orderBookFilter.getAmount() = " + orderBookFilter.getAmount() +
                        ", offer.getAmount() = " + offer.getAmount());
                log.debug("orderBookFilter.getDirection() = " + orderBookFilter.getDirection() +
                        ", offer.getDirection() = " + offer.getDirection());
                log.debug("orderBookFilter.getPrice() = " + orderBookFilter.getPrice() +
                        ", offer.getPrice() = " + offer.getPrice());
                log.debug("offer.getArbitrator() = " + offer.getArbitrator() +
                        ", settings.getAcceptedArbitrators() = " + settings.getAcceptedArbitrators());
                             */
                return result;
            }
        });
    }


    ///////////////////////////////////////////////////////////////////////////////////////////
    // Getter
    ///////////////////////////////////////////////////////////////////////////////////////////

    public SortedList<OrderBookListItem> getOfferList()
    {
        return offerList;
    }

    ///////////////////////////////////////////////////////////////////////////////////////////
    // Private Methods
    ///////////////////////////////////////////////////////////////////////////////////////////

    private boolean countryInList(Locale localeToMatch, List<Locale> list)
    {
        for (Locale locale : list)
        {
            if (locale.getCountry().equals(localeToMatch.getCountry()))
                return true;
        }
        return false;
    }

    private boolean languagesInList(List<Locale> list1, List<Locale> list2)
    {
        for (Locale locale1 : list2)
        {
            for (Locale locale2 : list1)
            {
                if (locale1.getLanguage().equals(locale2.getLanguage()))
                    return true;
            }
        }
        return false;
    }

    private boolean arbitratorInList(Arbitrator arbitratorToMatch, List<Arbitrator> list)
    {
        if (arbitratorToMatch != null)
        {
            for (Arbitrator arbitrator : list)
            {
                try
                {
                    if (arbitrator.getUID().equals(arbitratorToMatch.getUID()))
                        return true;
                } catch (Exception e)
                {
                    log.error(e.toString());
                }
            }
        }
        return false;
    }
}
